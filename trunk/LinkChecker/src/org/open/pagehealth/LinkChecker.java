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
import org.w3c.dom.Comment;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Example program to list links from a URL.
 */
public class LinkChecker {


    public static final String     PROPS_FILE       = "linkchecker.properties";
    public static final Properties PROPERTIES       = loadProperties();
    public static final String     DEFAULT_ROOT_URL =
            PROPERTIES.getProperty("org.open.pagehealth.default.page", "http://ebay.com");

    private static final Logger LOG                = Logger.getLogger(LinkChecker.class);
    private static final int    POOL_SIZE          = getIntProperty("org.open.pagehealth.poolsize", 10);
    public static final  int    CONNECTION_TIMEOUT = getIntProperty("org.open.pagehealth.timeout", 10000);
    public static final  int    RESULT_COL_WIDTH   = getIntProperty("org.open.pagehealth.result.width", 35);
    private static final String REPORT_FILE        =
            PROPERTIES.getProperty("org.open.pagehealth.reportfile", "report.html");

    public static final String PROXY = PROPERTIES.getProperty("org.open.pagehealth.proxy", null);

    public static final String USER_AGENT_STRING =
            PROPERTIES.getProperty("org.open.pagehealth.user-agent", "Mozilla/5.0");

    public static final boolean HEAD_REQUEST = getBooleanProperty("org.open.pagehealth.quick-check-head", false);

    private PageLink _rootPage;
    private long     _scanTime, _startTime;

    public static Properties loadProperties() {
        //System.setProperty("log4j.debug", "true");
        Properties props = null;
        final File propsFile = new File(PROPS_FILE);
        try {
            if (!propsFile.exists()) {
                propsFile.createNewFile();
                System.out.println("New File created. " + propsFile.getAbsolutePath());
            }
            props = new Properties();
            props.load(new FileInputStream(PROPS_FILE));
            System.out.println("Using properties file: " + propsFile.getAbsolutePath());
            System.setProperty("log4j.configuration", propsFile.toURI().toString());
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        return props;
    }

    public static int getIntProperty(final String key, final int defaultValue) {
        final String strValue = PROPERTIES.getProperty(key, null);
        if (null == strValue) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(strValue);
        } catch (Exception exp) {
            LOG.error("Invalid property value (un-parsable) value:" + strValue + ", key=" + key, exp);
            return defaultValue;
        }
    }

    public static boolean getBooleanProperty(final String key, final boolean defaultValue) {
        final String strValue = PROPERTIES.getProperty(key, null);
        if (null == strValue) {
            return defaultValue;
        }
        try {
            return Boolean.parseBoolean(strValue);
        } catch (Exception exp) {
            LOG.error("Invalid property value (un-parsable) value:" + strValue + ", key=" + key, exp);
            return defaultValue;
        }
    }


    public static void main(final String[] args) throws Exception {
        String rootURL = PROPERTIES.getProperty("org.open.pagehealth.page", null);
        if (rootURL == null) {
            rootURL = getPageUrlFromConsole();
        }
        new LinkChecker(validateURL(rootURL));
    }

    public LinkChecker(final String rootLink) {
        init(rootLink);
    }

    public void init(final String rootLink) {
        _rootPage = new PageLink(rootLink, "Root", "Root");
        _startTime = System.currentTimeMillis();
        final HashMap<String, PageLink> pageLinks = getLinksNodes(_rootPage);
        checkLinks(pageLinks);
        _scanTime = (System.currentTimeMillis() - _startTime);
        LOG.info("Scan duration: " + _scanTime + " ms with " + POOL_SIZE + " threads.");
        printResult(pageLinks);
    }

    public static String validateURL(String rootURL) {

        if (rootURL == null || rootURL.trim().length() < 1) {
            return null;
        }
        try {
            if (!rootURL.startsWith("http")) {
                rootURL = "http://" + rootURL;
            }
            final URL userURL = new URL(rootURL);
            userURL.getContent();
            return userURL.toString();
        } catch (MalformedURLException exp) {
            LOG.error("Invalid URL entered. Message: " + exp.getMessage());
        } catch (IOException exp) {
            LOG.error("Invalid URL entered. Message: ", exp);
        }
        return null;
    }

    public static String getPageUrlFromConsole() {
        try {
            final ConsoleReader reader = new ConsoleReader();
            System.out.print("Enter a website URL [" + DEFAULT_ROOT_URL + "]?: ");

            String line = reader.readLine();
            if (line.trim().length() == 0) {
                line = DEFAULT_ROOT_URL;
            }
            line = validateURL(line);
            if (null != line) {
                return line;
            }
            System.out.println("Please try again...");
            return getPageUrlFromConsole();

        } catch (IOException exp) {
            LOG.error("Invalid URL entered. Message: ", exp);
        }
        System.out.println("Please try again...");
        return getPageUrlFromConsole();
    }


    public HashMap<String, PageLink> getLinksNodes(final PageLink checkPage) {
        final long startTime = System.currentTimeMillis();
        try {
            LOG.info("Scanning page: " + checkPage.getURL());
            final Connection conn = Jsoup.connect(checkPage.getURL());
            final Document doc = conn.get();
            final Elements links = doc.select("a[href]");
            final Elements media = doc.select("[src]");
            final Elements imports = doc.select("link[href]");
            LOG.info("Page contains: Links: " + links.size() + ", Media: " + media.size() + ", " + "Imports: " +
                             imports.size());
            HashMap<String, PageLink> pageLinks =
                    new HashMap<String, PageLink>(links.size() + media.size() + imports.size());
            pageLinks = appendElements(pageLinks, links, "abs:href");
            pageLinks = appendElements(pageLinks, media, "abs:src");
            pageLinks = appendElements(pageLinks, imports, "abs:href");

            LOG.debug("Root page scan took " + (System.currentTimeMillis() - startTime) + " ms");
            return pageLinks;
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        return null;
    }

    private HashMap<String, PageLink> appendElements(final HashMap<String, PageLink> pageLinkList,
                                                     final Elements elem,
                                                     final String attrKey) {
        for (final Element pageElement : elem) {
            final String linkTarget = pageElement.attr(attrKey);
            if (linkTarget == null || linkTarget.trim().length() < 1) {
                final String href = pageElement.attr("href");
                if (null != href && href.startsWith("javascript:")) {
                    LOG.debug("Skipping javascript link: " + pageElement);
                    continue;
                }
                LOG.error("Empty link found" + pageElement);
            }
            if (linkTarget.startsWith("mailto")) {
                LOG.info("ignoring mailto link: " + pageElement);
                continue;
            }
            final String caption = pageElement.hasText() ? pageElement.text() : pageElement.attr("alt");
            pageLinkList.put(linkTarget,
                             new PageLink(linkTarget, trim(caption, RESULT_COL_WIDTH),
                                          pageElement.tag().toString()));
            //print(" * a: <%s>  (%s)", li, trim(link.text(), 35));
        }
        return pageLinkList;
    }

    public void checkLinks(final HashMap<String, PageLink> pageLinks) {


        if (null == pageLinks || pageLinks.size() < 1) {
            LOG.error("No pageLinks found on the page");
            return;
        }
        /*
        for (org.open.pagehealth.PageLink node : pageLinks) {
            Thread th = new Thread(new org.open.pagehealth.ClickThread(node));
            th.start();
        }*/


        final BlockingQueue<Runnable> worksQueue = new ArrayBlockingQueue<Runnable>(pageLinks.size());
        final RejectedExecutionHandler executionHandler = new TaskOverflowHandler();
        LOG.info("Starting Pool: Threads: " + POOL_SIZE);
        // Create the ThreadPoolExecutor
        final ThreadPoolExecutor executor =
                new ThreadPoolExecutor(POOL_SIZE, POOL_SIZE, 3, TimeUnit.SECONDS, worksQueue, executionHandler);
        executor.allowCoreThreadTimeOut(true);

        final Object callback = new Object();

        // Starting the monitor thread as a daemon
        final Thread monitor = new Thread(new TasksMonitorThread(executor, callback), "TasksMonitorThread");
        monitor.setDaemon(true);
        monitor.start();
        final Iterator<PageLink> pageLinkIterator = pageLinks.values().iterator();
        int counter = 1;
        // Adding the tasks
        while (pageLinkIterator.hasNext()) {
            final PageLink pLink = pageLinkIterator.next();
            executor.execute(new ClickThread("" + counter++, pLink));
        }
        try {
            synchronized (callback) {
                LOG.debug("Going to sleep until all tasks are complete");
                callback.wait();
            }
        } catch (Exception exp) {
            LOG.error("Exception in wait", exp);
        }
        executor.shutdown();
        LOG.debug("Is pool stopped: " + executor.isShutdown());
        LOG.info("Pool shutdown.");
    }

    public void printResult(final HashMap<String, PageLink> pageLinks) {
        try {
            /////////////////////////////
            //Creating an empty XML Document

            //We need a Document
            final DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            final DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
            final org.w3c.dom.Document doc = docBuilder.newDocument();

            ////////////////////////
            //Creating the XML tree

            //create the root element and add it to the document
            final org.w3c.dom.Element rootResultElement = doc.createElement("result");
            doc.appendChild(rootResultElement);

            //create a comment and put it in the root element
            final Comment comment = doc.createComment("LinkChecker application report file");
            rootResultElement.appendChild(comment);

            //create child element, add an attribute, and add to root
            final org.w3c.dom.Element pageElement = doc.createElement("page");
            pageElement.setAttribute("url", _rootPage.getURL());
            rootResultElement.appendChild(pageElement);

            // links node
            final org.w3c.dom.Element linksElement = doc.createElement("links");

            int goodLinks = 0;

            final Hashtable<String, Integer> threadInfo = new Hashtable<String, Integer>();

            for (final PageLink page : pageLinks.values()) {

                if (page.isGood()) { goodLinks++; }

                final String thName = page.getVerifiedThread();
                if (threadInfo.containsKey(thName)) {
                    Integer jobCount = threadInfo.get(thName);
                    jobCount = jobCount + 1;
                    threadInfo.put(thName, jobCount);
                } else {
                    threadInfo.put(thName, 1);
                }

                final org.w3c.dom.Element linkElement = doc.createElement("link");


                final org.w3c.dom.Element shortUrlElement2 = doc.createElement("short-url");
                shortUrlElement2.appendChild(doc.createTextNode(page.getShortURL()));
                linkElement.appendChild(shortUrlElement2);

                final org.w3c.dom.Element urlElement = doc.createElement("url");
                urlElement.appendChild(doc.createTextNode(page.getURL()));
                linkElement.appendChild(urlElement);

                final org.w3c.dom.Element typeElement = doc.createElement("type");
                typeElement.appendChild(doc.createTextNode(page.getType()));
                linkElement.appendChild(typeElement);

                final org.w3c.dom.Element captionElement = doc.createElement("caption");
                captionElement.appendChild(doc.createTextNode(page.getCaption()));
                linkElement.appendChild(captionElement);

                final org.w3c.dom.Element statusElement = doc.createElement("status");
                statusElement.appendChild(doc.createTextNode(page.getDisplayStatus()));
                linkElement.appendChild(statusElement);

                final org.w3c.dom.Element verifiedElement = doc.createElement("verified");
                verifiedElement.appendChild(doc.createTextNode(page.getDisplayVerifiedStatus()));
                linkElement.appendChild(verifiedElement);

                final org.w3c.dom.Element responseElement = doc.createElement("http-response");
                responseElement.appendChild(doc.createTextNode(page.getResponseStatus()));
                linkElement.appendChild(responseElement);

                final org.w3c.dom.Element contentTypeElement = doc.createElement("content-type");
                contentTypeElement.appendChild(doc.createTextNode(page.getContentType()));
                linkElement.appendChild(contentTypeElement);

                final org.w3c.dom.Element contentLengthElement = doc.createElement("content-length");
                contentLengthElement.appendChild(doc.createTextNode(page.getContentLength()));
                linkElement.appendChild(contentLengthElement);

                final org.w3c.dom.Element scanTimeElement = doc.createElement("scan-time");
                scanTimeElement.appendChild(doc.createTextNode(page.getScanTime()));
                linkElement.appendChild(scanTimeElement);

                final org.w3c.dom.Element threadElement = doc.createElement("verified-thread");
                threadElement.appendChild(doc.createTextNode(page.getVerifiedThread()));
                linkElement.appendChild(threadElement);
                linksElement.appendChild(linkElement);


            }
            pageElement.appendChild(linksElement);


            final org.w3c.dom.Element summaryElement = doc.createElement("summary");


            org.w3c.dom.Element child = doc.createElement("root-url");
            child.appendChild(doc.createTextNode(_rootPage.getURL()));
            summaryElement.appendChild(child);

            child = doc.createElement("link-count");
            child.appendChild(doc.createTextNode("" + pageLinks.size()));
            summaryElement.appendChild(child);

            child = doc.createElement("health");
            final String health = "" + ((float) goodLinks / pageLinks.size()) * 100;
            child.appendChild(doc.createTextNode(health));
            summaryElement.appendChild(child);

            child = doc.createElement("scan-time");
            child.appendChild(doc.createTextNode("" + _scanTime));
            summaryElement.appendChild(child);

            child = doc.createElement("start-time");
            child.appendChild(doc.createTextNode(new Date(_startTime).toString()));
            summaryElement.appendChild(child);

            child = doc.createElement("thread-count");
            child.appendChild(doc.createTextNode("" + POOL_SIZE));
            summaryElement.appendChild(child);

            pageElement.appendChild(summaryElement);


            //thread-pool node
            int max_weight = 0;

            final Enumeration<String> keys = threadInfo.keys();
            while (keys.hasMoreElements()) {

                final Integer threadCount = threadInfo.get(keys.nextElement());
                if (threadCount > max_weight) {
                    max_weight = threadCount;
                }
            }

            final org.w3c.dom.Element poolStatsElement = doc.createElement("thread-pool");
            final org.w3c.dom.Element threadsElement = doc.createElement("threads");


            final Enumeration<String> threadNames = threadInfo.keys();
            while (threadNames.hasMoreElements()) {
                final String threadName = threadNames.nextElement();
                final org.w3c.dom.Element threadElement = doc.createElement("thread");

                child = doc.createElement("name");
                child.appendChild(doc.createTextNode(threadName));
                threadElement.appendChild(child);

                child = doc.createElement("job-count");
                final Integer jobCount = threadInfo.get(threadName);
                child.appendChild(doc.createTextNode("" + jobCount));
                threadElement.appendChild(child);

                child = doc.createElement("job-share");
                final String share = "" + (float) jobCount / (max_weight) * 100;
                child.appendChild(doc.createTextNode("" + share));
                threadElement.appendChild(child);

                threadsElement.appendChild(threadElement);
            }
            poolStatsElement.appendChild(threadsElement);
            pageElement.appendChild(poolStatsElement);

            /////////////////
            //Output the XML

            //set up a transformer
            final Transformer transformer =
                    TransformerFactory.newInstance().newTransformer(new StreamSource("report-format" + ".xsl"));
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            //create string from xml tree
            //StringWriter sw = new StringWriter();
            final File sw = new File(REPORT_FILE);
            final DOMSource source = new DOMSource(doc);
            transformer.transform(source, new StreamResult(sw));
            LOG.info("Attempting to open the report file: " + sw.getAbsolutePath());
            Desktop.getDesktop().open(sw);

        } catch (Exception exp) {
            LOG.error("Error generating report", exp);
        }
    }

    private String trim(final String s, final int width) {
        if (s.length() > width) { return s.substring(0, width - 1) + "."; } else { return s; }
    }
}

