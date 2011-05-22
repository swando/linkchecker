package org.open.pagehealth; /**
 * User: swapnilsapar
 * Date: 5/2/11
 * Time: 4:20 PM
 */

import jline.ConsoleReader;
import org.apache.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Example program to list links from a URL.
 */
public class LinkChecker {

    private static final Logger LOG       = Logger.getLogger(LinkChecker.class);
    private static final int    POOL_SIZE = 20;

    private PageLink _rootPage;
    private long     _scanTime;

    public static void main(String[] args) throws Exception {
        LinkChecker checker = new LinkChecker();
        checker.init();
    }

    public void init() {
        //String rootLink = getUserWebSite();
        String rootLink = "http://ebay.com";
        _rootPage = new PageLink(rootLink, "Root");
        long startTime = System.currentTimeMillis();
        ArrayList<PageLink> pageLinks = getLinksNodes(_rootPage);
        checkLinks(pageLinks);
        _scanTime = (System.currentTimeMillis() - startTime);
        LOG.info("Total time " + _scanTime + " ms");
        printResult(pageLinks);
    }

    public String getUserWebSite() {
        try {
            ConsoleReader reader = new ConsoleReader();
            System.out.print("Enter a website URL [http://ebay.com]?: ");
            String line = reader.readLine();

            if (line != null && line.trim().length() > 1) {
                if (!line.startsWith("http")) {
                    line = "http://" + line;
                }
                URL userURL = new URL(line);
                userURL.getContent();
                return userURL.toString();
            }
            return "http://ebay.com";

        } catch (MalformedURLException exp) {
            LOG.error("Invalid URL entered. Message: " + exp.getMessage());
        } catch (IOException exp) {
            LOG.error("Invalid URL entered. Message: ", exp);
        }
        System.out.println("Please try again...");
        return getUserWebSite();
    }

    public void printResult(ArrayList<PageLink> pageLinks) {
        try {
            //write the content into xml file
            FileWriter fstream = new FileWriter("result.html");
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(
                    "<HTML><BODY><div style=\"overflow:auto\"><TABLE frame=\"box\">" + "<TR BGCOLOR=\"#82CAFA\"><TH "
                            +
                            "colspan=\"7\"><H1>LinkChecker Result</H1></TH></TR>" + PageLink.toColumn());
            //Close the output stream

            int goodLinks = 0;
            for (PageLink page : pageLinks) {
                boolean isGood = page.isGood();
                if (isGood) {
                    goodLinks++;
                } else {
                    //LOG.warn("Broken link: " + page);
                }
                //System.out.println(page.toRow());
                out.write(page.toRow());
            }
            LOG.info("Result: " + goodLinks + "/" + pageLinks.size() + ", health: " +
                             ((float) goodLinks / pageLinks.size()) * 100 + "%");

            out.write("<TR BGCOLOR=\"#82CAFA\"><TH colspan=\"7\" align=\"left\"><H2><a href=\"" + _rootPage.getURL()
                              +
                              "\">" + _rootPage.getURL() + "</a>   Links: " + goodLinks + "/" + pageLinks.size() +
                              ", Health: " + ((float) goodLinks / pageLinks.size()) * 100 + "%, Scan Time: " +
                              _scanTime + " ms, " + POOL_SIZE + " Threads</H2></TH></TR>");
            out.write("</TABLE></BODY></HTML>");
            out.close();
        } catch (Exception exp) {
            LOG.error("Error generating report", exp);
        }
    }


    public ArrayList<PageLink> getLinksNodes(PageLink checkPage) {
        long startTime = System.currentTimeMillis();
        try {
            LOG.info("Scanning page: " + checkPage.getURL());
            Connection conn = Jsoup.connect(checkPage.getURL());
            Document doc = conn.get();
            Elements links = doc.select("a[href]");
            ArrayList<PageLink> pageLinks = new ArrayList<PageLink>(links.size());
            LOG.info("Links: " + links.size());
            for (Element link : links) {
                String li = link.attr("abs:href");
                pageLinks.add(new PageLink(li, trim(link.text(), 35)));
                //print(" * a: <%s>  (%s)", li, trim(link.text(), 35));
            }
            LOG.info("Page scan took " + (System.currentTimeMillis() - startTime) + " ms");
            return pageLinks;
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        return null;
    }

    public void checkLinks(ArrayList<PageLink> pageLinks) {


        if (null == pageLinks || pageLinks.size() < 1) {
            LOG.error("No pageLinks found on the page");
            return;
        }
        /*
        for (org.open.pagehealth.PageLink node : pageLinks) {
            Thread th = new Thread(new org.open.pagehealth.ClickThread(node));
            th.start();
        }*/


        BlockingQueue<Runnable> worksQueue = new ArrayBlockingQueue<Runnable>(pageLinks.size());
        RejectedExecutionHandler executionHandler = new TaskOverflowHandler();

        // Create the ThreadPoolExecutor
        ThreadPoolExecutor executor =
                new ThreadPoolExecutor(POOL_SIZE, POOL_SIZE, 3, TimeUnit.SECONDS, worksQueue, executionHandler);
        executor.allowCoreThreadTimeOut(true);

        Object callback = new Object();

        // Starting the monitor thread as a daemon
        Thread monitor = new Thread(new TasksMonitorThread(executor, callback), "TasksMonitorThread");
        monitor.setDaemon(true);
        monitor.start();
        // Adding the tasks
        for (int i = 0; i < pageLinks.size(); i++) {
            executor.execute(new ClickThread("" + i, pageLinks.get(i)));
        }
        try {
            synchronized (callback) {
                LOG.info("Going to sleep until all tasks are complete");
                callback.wait();
            }
        } catch (Exception exp) {
            LOG.error("Exception in wait", exp);
        }
        LOG.info("Getting ready to shutdown");
        executor.shutdown();
        LOG.info("Stopped the pool: " + executor.isShutdown());
    }

    private String trim(String s, int width) {
        if (s.length() > width) { return s.substring(0, width - 1) + "."; } else { return s; }
    }
}

