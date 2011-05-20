package org.open.pagehealth; /**
 * User: swapnilsapar
 * Date: 5/2/11
 * Time: 4:20 PM
 */

import org.apache.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Hashtable;
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
    private static final int    POOL_SIZE = 10;

    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();
        //Validate.isTrue(args.length == 1, "usage: supply url to fetch");
        //String url = args[0];
        //print("Fetching %s...", url);
        //getLinks("http://google.com");
        //getLinks("http://www.amazon.com/dnqwoqwoenqwlewqq");
        //getLinks("http://www.amazon.com/50490-Dimmer-Gradual-Touch-Control/dp/B000U1ZDTM");
        //getLinks("tired");
        //TreeMap<String, org.open.pagehealth.PageLink> map = new TreeMap<String,
        //org.open.pagehealth.PageLink>();
        //String rootLink = "http://www.bbc.com";
        //String rootLink = "http://htmlparser.sourceforge.net/samples.html";
        //String rootLink = "http://www.amazon.com/";
        //String rootLink = "http://www.hallmark.com/";
        //String rootLink = "http://hallmark.businessgreetings.com";
        String rootLink = "http://www.ebay.com/";
        //String rootLink = "http://www.apple.com/";

        ArrayList<PageLink> pagePageLinks = getLinksNodes(new PageLink(rootLink, "Root"));
        LinkChecker s = new LinkChecker();
        s.checkLinks(pagePageLinks);
        //Thread.currentThread().sleep(10000);
        s.printResult(pagePageLinks);
        LOG.info("Total time " + (System.currentTimeMillis() - startTime) + " ms");


    }


    public void printResult(ArrayList<PageLink> pageLinks) {

        Hashtable<String, Integer> linkStats = new Hashtable<String, Integer>(pageLinks.size());
        int goodLinks = 0;
        for (PageLink page : pageLinks) {
            boolean isGood = page.isGood();
            if (isGood) {
                //linkStats.get(page.
                goodLinks++;
            } else {
                LOG.warn("Broken link: " + page);
            }
        }
        LOG.info("Result: " + goodLinks + "/" + pageLinks.size() + ", health: " + ((float) goodLinks / pageLinks.size()) *
        100 + "%");
    }


    public static ArrayList<PageLink> getLinksNodes(PageLink checkPage) {
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
        Thread monitor = new Thread(new TasksMonitorThread(executor, callback), "Monitor");
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
        LOG.info("Got notified");
        executor.shutdown();
        LOG.info("Stopped the pool: " + executor);
    }


    private static void print(String msg, Object... args) {
        LOG.info(String.format(msg, args));
    }

    private static String trim(String s, int width) {
        if (s.length() > width)
            return s.substring(0, width - 1) + ".";
        else
            return s;
    }
}

