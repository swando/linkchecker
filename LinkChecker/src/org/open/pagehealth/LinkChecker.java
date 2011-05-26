package org.open.pagehealth; /**
 * User: swapnilsapar
 * Date: 5/2/11
 * Time: 4:20 PM
 */

import jline.ConsoleReader;
import org.apache.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
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
            PROPERTIES.getProperty("org.open.pagehealth.reportfile", "report.xml");

    public static final String PROXY = PROPERTIES.getProperty("org.open.pagehealth.proxy", null);

    public static final String USER_AGENT_STRING =
            PROPERTIES.getProperty("org.open.pagehealth.user-agent", "Mozilla/5.0");

    public static final boolean HEAD_REQUEST = getBooleanProperty("org.open.pagehealth.quick-check-head", false);

    private PageLink _rootPage;
    private long     _scanTime, _startTime;

    public static Properties loadProperties() {
        //System.setProperty("log4j.debug", "true");
        Properties props = null;
        File propsFile = new File(PROPS_FILE);
        try {
            if (!propsFile.exists()) {
                propsFile.createNewFile();
                System.out.println("New File created. " + propsFile.getAbsolutePath());
            }
            props = new Properties();
            props.load(new FileInputStream(PROPS_FILE));
            System.out.println("Props File loaded. " + propsFile.getAbsolutePath());
            System.setProperty("log4j.configuration", "file://" + propsFile.getAbsolutePath());
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        return props;
    }

    public static int getIntProperty(String key, int defaultValue) {
        String strValue = PROPERTIES.getProperty(key, null);
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

    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String strValue = PROPERTIES.getProperty(key, null);
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

    public LinkChecker(String rootLink) {
        init(rootLink);
    }

    public void init(final String rootLink) {
        _rootPage = new PageLink(rootLink, "Root", "Root");
        _startTime = System.currentTimeMillis();
        final ArrayList<PageLink> pageLinks = getLinksNodes(_rootPage);
        checkLinks(pageLinks);
        _scanTime = (System.currentTimeMillis() - _startTime);
        LOG.info("Total time " + _scanTime + " ms");
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


    public ArrayList<PageLink> getLinksNodes(final PageLink checkPage) {
        final long startTime = System.currentTimeMillis();
        try {
            LOG.info("Scanning root page: " + checkPage.getURL());
            final Connection conn = Jsoup.connect(checkPage.getURL());
            final Document doc = conn.get();
            Elements links = doc.select("a[href]");
            LOG.info("Links: " + links.size());

            Elements media = doc.select("[src]");
            LOG.info("media: " + media.size());

            Elements imports = doc.select("link[href]");
            LOG.info("imports: " + imports.size());

            ArrayList<PageLink> pageLinks = new ArrayList<PageLink>(links.size() + media.size() + imports.size());
            pageLinks = appendElements(pageLinks, links, "abs:href", "Link");
            pageLinks = appendElements(pageLinks, media, "abs:src", "Media");
            pageLinks = appendElements(pageLinks, imports, "abs:href", "Import");

            LOG.info("Root page scan took " + (System.currentTimeMillis() - startTime) + " ms");
            return pageLinks;
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        return null;
    }

    private ArrayList<PageLink> appendElements(ArrayList<PageLink> pageLinkList, Elements elem, String attrKey,
                                               String type) {
        for (final Element pageElement : elem) {
            final String linkTarget = pageElement.attr(attrKey);
            if (linkTarget == null || linkTarget.trim().length() < 1) {
                String href = pageElement.attr("href");
                if (null != href && href.startsWith("javascript:")) {
                    LOG.warn("Skipping over javascript link" + pageElement);
                    continue;
                }
                LOG.error("Empty link found" + pageElement);
            }
            if (linkTarget.startsWith("mailto")) {
                LOG.info("ignoring mailto link: " + pageElement);
                continue;
            }
            pageLinkList.add(new PageLink(linkTarget, trim(pageElement.text(), RESULT_COL_WIDTH), type));
            //print(" * a: <%s>  (%s)", li, trim(link.text(), 35));
        }
        return pageLinkList;
    }

    public void checkLinks(final ArrayList<PageLink> pageLinks) {


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

        // Create the ThreadPoolExecutor
        final ThreadPoolExecutor executor =
                new ThreadPoolExecutor(POOL_SIZE, POOL_SIZE, 3, TimeUnit.SECONDS, worksQueue, executionHandler);
        executor.allowCoreThreadTimeOut(true);

        final Object callback = new Object();

        // Starting the monitor thread as a daemon
        final Thread monitor = new Thread(new TasksMonitorThread(executor, callback), "TasksMonitorThread");
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

    public void printResult(final ArrayList<PageLink> pageLinks) {
        try {
            final FileOutputStream fos = new FileOutputStream(REPORT_FILE);
            /*

            DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();

            DOMImplementationLS domImplementationLS = (DOMImplementationLS)registry.getDOMImplementation("LS");
            ///

            LSSerializer lsSerializer = domImplementationLS.createLSSerializer();

             DOMConfiguration domConfiguration = lsSerializer.getDomConfig();
          if (domConfiguration.canSetParameter("format-pretty-print", Boolean.TRUE)) {
             lsSerializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
             LSOutput lsOutput = domImplementationLS.createLSOutput();
             lsOutput.setEncoding("UTF-8");
             StringWriter stringWriter = new StringWriter();
             lsOutput.setCharacterStream(stringWriter);
             lsSerializer.write(document, lsOutput);
          }

            String str = writer.writeToString(document);
            */

            // XERCES 1 or 2 additionnal classes.
            final OutputFormat of = new OutputFormat("XML", "ISO-8859-1", true);
            of.setIndent(1);
            of.setIndenting(true);
            //of.setDoctype(null, "report-format.dtd");
            final XMLSerializer serializer = new XMLSerializer(fos, of);
            // SAX2.0 ContentHandler.
            final ContentHandler hd = serializer.asContentHandler();
            hd.startDocument();
            // Processing instruction sample.
            hd.processingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"report-format.xsl\"");
            // USER attributes.

            // USERS tag.
            hd.startElement("", "", "result", null);
            final AttributesImpl pageAttributes = new AttributesImpl();
            pageAttributes.addAttribute("", "", "url", "CDATA", _rootPage.getURL());
            hd.startElement(null, null, "page", pageAttributes);

            // links node
            hd.startElement(null, null, "links", null);
            int goodLinks = 0;

            final Hashtable<String, Integer> threadInfo = new Hashtable<String, Integer>();

            for (final PageLink page : pageLinks) {
                hd.startElement(null, null, "link", null);

                hd.startElement(null, null, "short-url", null);
                hd.characters(page.getShortURL().toCharArray(), 0, page.getShortURL().length());
                hd.endElement("", "", "short-url");

                hd.startElement(null, null, "url", null);
                hd.characters(page.getURL().toCharArray(), 0, page.getURL().length());
                hd.endElement("", "", "url");

                hd.startElement(null, null, "type", null);
                hd.characters(page.getType().toCharArray(), 0, page.getType().length());
                hd.endElement("", "", "type");

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

                final String thName = page.getVerifiedThread();
                if (threadInfo.containsKey(thName)) {
                    Integer jobCount = threadInfo.get(thName);
                    jobCount = jobCount.intValue() + 1;
                    threadInfo.put(thName, jobCount);
                } else {
                    threadInfo.put(thName, 1);
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

            hd.startElement(null, null, "link-count", null);
            String count = "" + pageLinks.size();
            hd.characters(count.toCharArray(), 0, count.length());
            hd.endElement("", "", "link-count");

            hd.startElement(null, null, "health", null);
            final String health = "" + ((float) goodLinks / pageLinks.size()) * 100;
            hd.characters(health.toCharArray(), 0, health.length());
            hd.endElement("", "", "health");

            hd.startElement(null, null, "scan-time", null);
            String time = "" + _scanTime;
            hd.characters(time.toCharArray(), 0, time.length());
            hd.endElement("", "", "scan-time");

            hd.startElement(null, null, "start-time", null);
            time = new Date(_startTime).toString();
            hd.characters(time.toCharArray(), 0, time.length());
            hd.endElement("", "", "start-time");


            hd.startElement(null, null, "thread-count", null);
            hd.characters(("" + POOL_SIZE).toCharArray(), 0, ("" + POOL_SIZE).length());
            hd.endElement("", "", "thread-count");
            hd.endElement("", "", "summary");

            //thread-pool node
            int max_weight = 0;

            final Enumeration<String> keys = threadInfo.keys();
            while (keys.hasMoreElements()) {

                final Integer threadCount = threadInfo.get(keys.nextElement());
                if (threadCount > max_weight) {
                    max_weight = threadCount;
                }
            }


            hd.startElement("", "", "thread-pool", null);
            hd.startElement(null, null, "threads", null);
            final Enumeration<String> threadNames = threadInfo.keys();
            while (threadNames.hasMoreElements()) {

                hd.startElement(null, null, "thread", null);

                final String threadName = threadNames.nextElement();

                hd.startElement(null, null, "name", null);
                hd.characters(threadName.toCharArray(), 0, threadName.length());
                hd.endElement("", "", "name");

                hd.startElement(null, null, "job-count", null);
                final Integer jobCount = threadInfo.get(threadName);
                final String jCount = "" + jobCount;
                hd.characters(jCount.toCharArray(), 0, jCount.length());
                hd.endElement("", "", "job-count");

                hd.startElement(null, null, "job-share", null);
                final String share = "" + (float) jobCount / (max_weight) * 100;
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
            LOG.info("Generated report file at: " + new File(REPORT_FILE).getAbsolutePath());

        } catch (Exception exp) {
            LOG.error("Error generating report", exp);
        }
    }

    private String trim(final String s, final int width) {
        if (s.length() > width) { return s.substring(0, width - 1) + "."; } else { return s; }
    }
}

