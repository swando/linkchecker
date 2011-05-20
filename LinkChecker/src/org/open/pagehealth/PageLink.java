package org.open.pagehealth;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

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
    private long    _contentLenght;


    public PageLink(String link, String caption) {
        _link = link;
        _caption = caption;
    }

    public String getURL() {
        return _link;
    }


    public boolean isGood() {
        return _isGood;
    }

    private void setResult(boolean isGood, String errorMsg) {
        _isVerified = true;
        _isGood = isGood;
        _responseStatus = errorMsg;
    }

    private void setBroken(String errorMsg) {
        setResult(false, errorMsg);
    }

    private void setGood(String response, String contentType, long contentLength) {
        setResult(true, response);
        _contentLenght = contentLength;
        _contentType = contentType;
    }

    public void checkLink() {
        LOG.debug("Checking _link: " + _link);
        if (null == _link || _link.trim().length() < 1) {
            setBroken("Empty Link");
            return;
        }

        HttpResponse response;
        try {
            HttpHead method = new HttpHead(_link);
            method.addHeader("User-Agent", "Mozilla");
            HttpClient client = getHttpClient();
            response = client.execute(method);
            StatusLine line = response.getStatusLine();
            if (line.getStatusCode() == 404) {
                setBroken(line.toString());
            } else {
                String contentType = response.getLastHeader(HttpHeaders.CONTENT_TYPE).toString();
                Header lengthHeader = response.getLastHeader(HttpHeaders.CONTENT_LENGTH);
                long contentLengh = 0;
                if (null != lengthHeader) {
                    contentLengh = Long.parseLong(lengthHeader.getValue());
                }

                setGood(line.toString(), contentType, contentLengh);
            }
        } catch (Exception exp) {

            String msg = "Link failed: " + _caption + " via " + _link + ", Exception" + exp;
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
                _isVerified + ", _contentType='" + _contentType + '\'' + ", _contentLenght=" + _contentLenght + '}';
    }

    private static Scheme getTrustAllScheme() {
        try {
            // Now you are telling the JRE to trust any https server.
            // If you know the URL that you are connecting to then this should not be a problem
            //trustAllHttpsCertificates
            //  Create a trust manager that does not validate certificate chains:
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1];
            javax.net.ssl.TrustManager tm = new EnvTrustManager();
            trustAllCerts[0] = tm;
            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, null);
            org.apache.http.conn.ssl.SSLSocketFactory sf =
                new org.apache.http.conn.ssl.SSLSocketFactory(sc,
                    org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            //create scheme for apache HttpClient
            SCHEME = new Scheme("https", 443, sf);


            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String urlHostName, SSLSession session) {
                    System.out.println("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
                    return true;
                }
            });

            // trust all HttpUnit Connection
            com.sun.net.ssl.internal.www.protocol.https.HttpsURLConnectionOldImpl
                    .setDefaultHostnameVerifier(new com.sun.net.ssl.HostnameVerifier() {
                        public boolean verify(String urlHostname, String certHostname) {
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
        DefaultHttpClient client = new DefaultHttpClient();
        client.getConnectionManager().getSchemeRegistry().register(SCHEME);
        return client;
    }
    // Just add these two functions in your program

    public static class EnvTrustManager implements javax.net.ssl.TrustManager, javax.net.ssl.X509TrustManager {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public boolean isServerTrusted(java.security.cert.X509Certificate[] certs) {
            return true;
        }

        public boolean isClientTrusted(java.security.cert.X509Certificate[] certs) {
            return true;
        }

        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType)
                throws java.security.cert.CertificateException {
            return;
        }

        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType)
                throws java.security.cert.CertificateException {
            return;
        }
    }
}
