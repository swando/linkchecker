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

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * Example program to list links from a URL.
 */
public class LinkChecker {

    private static final Logger LOG       = Logger.getLogger(LinkChecker.class);
    private static final int    POOL_SIZE = 20;
    public static final int    CONNECTION_TIMEOUT = 10000;

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
            String fileName = "report.xml";

            FileOutputStream fos = new FileOutputStream(fileName);
            // XERCES 1 or 2 additionnal classes.
            OutputFormat of = new OutputFormat("XML", "ISO-8859-1", true);
            of.setIndent(1);
            of.setIndenting(true);
            //of.setDoctype(null, "report-format.dtd");
            XMLSerializer serializer = new XMLSerializer(fos, of);
            // SAX2.0 ContentHandler.
            ContentHandler hd = serializer.asContentHandler();
            hd.startDocument();
            // Processing instruction sample.
            hd.processingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"report-format.xsl\"");
            // USER attributes.

            AttributesImpl atts = new AttributesImpl();
            // USERS tag.
            hd.startElement("", "", "result", null);
            AttributesImpl pageAttributes = new AttributesImpl();
            pageAttributes.addAttribute("", "", "url", "CDATA", _rootPage.getURL());
            hd.startElement(null, null, "page", pageAttributes);

            // links node
            hd.startElement(null, null, "links", null);
            int goodLinks = 0;

            Hashtable<String, Integer> threadInfo = new Hashtable<String, Integer>();

            for (PageLink page : pageLinks) {
                hd.startElement(null, null, "link", null);

                hd.startElement(null, null, "short-url", null);
                hd.characters(page.getShortURL().toCharArray(), 0, page.getShortURL().length());
                hd.endElement("", "", "short-url");

                hd.startElement(null, null, "url", null);
                hd.characters(page.getURL().toCharArray(), 0, page.getURL().length());
                hd.endElement("", "", "url");

                hd.startElement(null, null, "caption", null);
                hd.characters(page.getCaption().toCharArray(), 0, page.getCaption().length());
                hd.endElement("", "", "caption");


                if (page.isGood()) { goodLinks++; }
                hd.startElement(null, null, "status", null);
                hd.characters(page.getDisplayStatus().toCharArray(), 0, page.getDisplayStatus().length());
                hd.endElement("", "", "status");

                hd.startElement(null, null, "verified", null);
                hd.characters(page.getDisplayVerifiedStatus().toCharArray(), 0,
                              page.getDisplayVerifiedStatus().length());
                hd.endElement("", "", "verified");

                hd.startElement(null, null, "http-response", null);
                hd.characters(page.getResponseStatus().toCharArray(), 0, page.getResponseStatus().length());
                hd.endElement("", "", "http-response");

                hd.startElement(null, null, "content-type", null);
                hd.characters(page.getContentType().toCharArray(), 0, page.getContentType().length());
                hd.endElement("", "", "content-type");

                hd.startElement(null, null, "content-length", null);
                hd.characters(page.getContentLength().toCharArray(), 0, page.getContentLength().length());
                hd.endElement("", "", "content-length");

                hd.startElement(null, null, "scan-time", null);
                hd.characters(page.getScanTime().toCharArray(), 0, page.getScanTime().length());
                hd.endElement("", "", "scan-time");

                String thName = page.getVerifiedThread();
                if (threadInfo.containsKey(thName)) {
                    Integer jobCount = threadInfo.get(thName);
                    jobCount = new Integer(jobCount.intValue() + 1);
                    threadInfo.put(thName, jobCount);
                } else {
                    threadInfo.put(thName, new Integer(1));
                }

                hd.startElement(null, null, "verified-thread", null);
                hd.characters(thName.toCharArray(), 0, thName.length());
                hd.endElement("", "", "verified-thread");

                hd.endElement("", "", "link");
            }
            hd.endElement("", "", "links");

            // summary node
            hd.startElement("", "", "summary", null);

            hd.startElement(null, null, "root-url", null);
            hd.characters(_rootPage.getURL().toCharArray(), 0, _rootPage.getURL().length());
            hd.endElement("", "", "root-url");

            hd.startElement(null, null, "health", null);
            String health = ""+((float) goodLinks / pageLinks.size()) * 100;
            hd.characters(health.toCharArray(), 0, health.length());
            hd.endElement("", "", "health");

            hd.startElement(null, null, "scan-time", null);
            String scanTime = "" + _scanTime;
            hd.characters(scanTime.toCharArray(), 0, scanTime.length());
            hd.endElement("", "", "scan-time");

            hd.startElement(null, null, "thread-count", null);
            hd.characters(("" + POOL_SIZE).toCharArray(), 0, ("" + POOL_SIZE).length());
            hd.endElement("", "", "thread-count");
            hd.endElement("", "", "summary");

            //thread-pool node
            int max_weight = 0;

            Enumeration<String> keys = threadInfo.keys();
            while(keys.hasMoreElements()) {

            Integer count = threadInfo.get(keys.nextElement());
                if(count > max_weight) {
                   max_weight = count;
                }
            }


            hd.startElement("", "", "thread-pool", null);
            hd.startElement(null, null, "threads", null);
            Enumeration<String> threadNames = threadInfo.keys();
            while(threadNames.hasMoreElements()) {

                hd.startElement(null, null, "thread", null);

                String threadName = threadNames.nextElement();

                hd.startElement(null, null, "name", null);
                hd.characters(threadName.toCharArray(), 0, threadName.length());
                hd.endElement("", "", "name");

                hd.startElement(null, null, "job-count", null);
                Integer jobCount = threadInfo.get(threadName);
                String count = "" + jobCount;
                hd.characters(count.toCharArray(), 0, count.length());
                hd.endElement("", "", "job-count");

                hd.startElement(null, null, "job-share", null);
                String share = ""+(float)jobCount/(max_weight)*100;
                hd.characters(share.toCharArray(), 0, share.length());
                hd.endElement("", "", "job-share");

                hd.endElement("", "", "thread");
            }
            System.out.println();

            hd.endElement("", "", "threads");
            hd.endElement("", "", "thread-pool");

            hd.endElement("", "", "page");
            hd.endElement("", "", "result");
            hd.endDocument();
            fos.close();

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

