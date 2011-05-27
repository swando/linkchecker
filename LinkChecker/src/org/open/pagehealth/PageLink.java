package org.open.pagehealth;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.log4j.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * User: swapnilsapar
 * Date: 5/2/11
 * Time: 5:03 PM
 */
public class PageLink {
    private static Scheme SCHEME;
    private static final Logger LOG = Logger.getLogger(PageLink.class);
    private String  _link;
    private String  _caption;
    private boolean _isGood;
    private String  _responseStatus;
    private boolean _isVerified;
    private String  _contentType;
    private long _contentLength = -1;
    private long   _scanTime;
    private Thread _thread;
    private String _type;

    public PageLink(final String link, final String caption, final String type) {
        _link = link;
        _caption = caption;
        _type = type;
    }

    public String getType() {
        return _type;
    }

    public String getURL() {
        return _link;
    }

    public String getResponseStatus() {
        return _responseStatus;
    }

    public String getContentType() {
        return _contentType == null ? "" : _contentType;
    }

    public String getContentLength() {
        return -1 == _contentLength ? "" : "" + _contentLength;
    }

    public String getScanTime() {
        return "" + _scanTime;
    }

    public String getVerifiedThread() {
        return _thread == null ? "" : _thread.getName();
    }

    public String getCaption() {
        return _caption;
    }


    public boolean isGood() {
        return _isGood;
    }

    private void setResult(final boolean isGood, final String errorMsg) {
        _isVerified = true;
        _isGood = isGood;
        _responseStatus = errorMsg;
    }

    private void setBroken(final String errorMsg) {
        setResult(false, errorMsg);
    }

    private void setGood(final String response, final String contentType, final long contentLength) {
        setResult(true, response);
        _contentLength = contentLength;
        _contentType = contentType;
    }

    public void checkLink() {
        _thread = Thread.currentThread();
        LOG.debug("Checking _link: " + _link);
        if (null == _link || _link.trim().length() < 1) {
            setBroken("Empty Link");
            return;
        }

        final HttpResponse response;
        long startTime = System.currentTimeMillis();
        try {
            _link = encodeQuery(_link);
            final HttpUriRequest method;
            if (LinkChecker.HEAD_REQUEST) {
                // shallow check via HTTP HEAD request
                method = new HttpHead(_link);
            } else {
                // deep check by downloading complete page
                method = new HttpGet(_link);
            }
            method.addHeader(HttpHeaders.USER_AGENT, LinkChecker.USER_AGENT_STRING);
            final HttpClient client = getHttpClient();
            startTime = System.currentTimeMillis();
            response = client.execute(method);
            _scanTime = (System.currentTimeMillis() - startTime);
            final StatusLine line = response.getStatusLine();
            if (line.getStatusCode() == 404) {
                setBroken(line.toString());
            } else {
                String contentType = "";
                final Header contentTypeHeader = response.getLastHeader(HttpHeaders.CONTENT_TYPE);
                if (null != contentTypeHeader && contentTypeHeader.getValue() != null) {
                    contentType = contentTypeHeader.getValue();
                }
                final Header lengthHeader = response.getLastHeader(HttpHeaders.CONTENT_LENGTH);
                long contentLengh = 0;
                if (null != lengthHeader) {
                    contentLengh = Long.parseLong(lengthHeader.getValue());
                }

                setGood(line.toString(), contentType, contentLengh);
            }
        } catch (Exception exp) {
            _scanTime = (System.currentTimeMillis() - startTime);
            String msg = "Link failed: " + _caption + " via " + _link + ", Exception: " + exp;
            if (exp.getCause() != null) {
                msg = msg + ", Cause: " + exp.getCause().getMessage();
            }
            LOG.error(msg, exp);
            setBroken(msg);
        } finally {
            // release connection, perform clean-up
            // getMethod
        }
    }

    @Override
    public String toString() {
        return "org.open.pagehealth.PageLink{" + "_link='" + _link + '\'' + ", _caption='" + _caption + '\'' +
                ", _isGood=" + _isGood + ", _responseStatus='" + _responseStatus + '\'' + ", _isVerified=" +
                _isVerified + ", _contentType='" + _contentType + '\'' + ", _contentLength=" + _contentLength + '}';
    }

    public String getShortURL() {
        if (null == _link) { return null; }

        if (_link.trim().length() < 1) {
            return "";
        }
        return _link.length() > LinkChecker.RESULT_COL_WIDTH ?
                _link.substring(0, LinkChecker.RESULT_COL_WIDTH) + "..." : _link;

    }

    public String getDisplayStatus() {
        return _isGood ? "Good" : "Broken";
    }

    public String getDisplayVerifiedStatus() {
        return _isVerified ? "Yes" : "No";
    }


    private static Scheme getTrustAllScheme() {
        try {
            // Now you are telling the JRE to trust any https server.
            // If you know the URL that you are connecting to then this should not be a problem
            //trustAllHttpsCertificates
            //  Create a trust manager that does not validate certificate chains:
            final javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1];
            final javax.net.ssl.TrustManager tm = new EnvTrustManager();
            trustAllCerts[0] = tm;
            final javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, null);
            final org.apache.http.conn.ssl.SSLSocketFactory sf = new org.apache.http.conn.ssl.SSLSocketFactory(sc,
                org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            //create scheme for apache HttpClient
            SCHEME = new Scheme("https", 443, sf);


            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(final String urlHostName, final SSLSession session) {
                    System.out.println("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
                    return true;
                }
            });

            // trust all HttpUnit Connection
            com.sun.net.ssl.internal.www.protocol.https.HttpsURLConnectionOldImpl
                    .setDefaultHostnameVerifier(new com.sun.net.ssl.HostnameVerifier() {
                        public boolean verify(final String urlHostname, final String certHostname) {
                            return true;
                        }
                    });
            /*
            checkUrl("http://google.com");
            checkUrl("https://encrypted.google.com");
            checkUrl("https://me.com/");
            checkUrl("http://p01-content.digitalhub.com");
            checkUrl("httpS://p01-content.digitalhub.com");
            checkUrl("httpS://17.230.129.184:443/show/health");
            */

        } catch (Exception exp) {
            exp.printStackTrace();
        }
        return SCHEME;
    }

    public static DefaultHttpClient getHttpClient() {
        if (null == SCHEME) {
            SCHEME = getTrustAllScheme();
        }
        final HttpParams params = new BasicHttpParams();
        params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, LinkChecker.CONNECTION_TIMEOUT);
        if (LinkChecker.PROXY != null) {
            final HttpHost proxyHost = new HttpHost(LinkChecker.PROXY.substring(0, LinkChecker.PROXY.indexOf(':')),
                                                    Integer.parseInt(LinkChecker.PROXY.substring(
                                                            LinkChecker.PROXY.indexOf(':') + 1)));
            params.setParameter(ConnRoutePNames.DEFAULT_PROXY, proxyHost);
        }

        //HttpClientParams.setRedirecting(params, false);
        final DefaultHttpClient client = new DefaultHttpClient(params);
        client.getConnectionManager().getSchemeRegistry().register(SCHEME);

        return client;
    }
    // Just add these two functions in your program

    public static class EnvTrustManager implements javax.net.ssl.TrustManager, javax.net.ssl.X509TrustManager {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public boolean isServerTrusted(final java.security.cert.X509Certificate[] certs) {
            return true;
        }

        public boolean isClientTrusted(final java.security.cert.X509Certificate[] certs) {
            return true;
        }

        public void checkServerTrusted(final java.security.cert.X509Certificate[] certs, final String authType)
                throws java.security.cert.CertificateException {
            return;
        }

        public void checkClientTrusted(final java.security.cert.X509Certificate[] certs, final String authType)
                throws java.security.cert.CertificateException {
            return;
        }
    }

    public static String encodeQuery(final String unEscaped) throws URISyntaxException {
        final int mark = unEscaped.indexOf('?');
        if (mark == -1) {
            return unEscaped;
        }
        final String[] parts = unEscaped.split(":");
        return new URI(parts[0], parts[1], null).toString();
    }
}
