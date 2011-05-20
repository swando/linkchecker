http://code.google.com/p/linkchecker/

ANT

Sample/Example run
/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/bin/java -Dlog4j.configuration=file:///Users/swapnilsapar/myprojects/linkchecker/LinkChecker/log4j.properties -Dfile.encoding=UTF-8 -classpath /System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/lib/deploy.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/lib/dt.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/lib/javaws.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/lib/jce.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/lib/management-agent.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/lib/plugin.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/lib/sa-jdi.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/../Classes/charsets.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/../Classes/classes.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/../Classes/dt.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/../Classes/jce.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/../Classes/jconsole.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/../Classes/jsse.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/../Classes/management-agent.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/../Classes/ui.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/lib/ext/apple_provider.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/lib/ext/dnsns.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/lib/ext/localedata.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/lib/ext/sunjce_provider.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/lib/ext/sunpkcs11.jar:/Users/swapnilsapar/myprojects/linkchecker/LinkChecker/out/production/LinkChecker:/Users/swapnilsapar/myprojects/linkchecker/LinkChecker/lib/log4j-1.2.16.jar:/Users/swapnilsapar/myprojects/linkchecker/LinkChecker/lib/jsoup-1.5.2.jar:/Users/swapnilsapar/myprojects/linkchecker/LinkChecker/lib/httpclient-4.1.1.jar:/Users/swapnilsapar/myprojects/linkchecker/LinkChecker/lib/httpcore-4.1.jar:/Users/swapnilsapar/myprojects/linkchecker/LinkChecker/lib/commons-logging-1.1.1.jar org.open.pagehealth.LinkChecker
 INFO [main] (LinkChecker.java:81) - Scanning page: http://www.ebay.com/
 INFO [main] (LinkChecker.java:86) - Links: 236
 INFO [main] (LinkChecker.java:92) - Page scan took 1095 ms
 INFO [Monitor] (TasksMonitorThread.java:24) - [monitor] [0/10] Active: 0, Completed: 0, Task: 0, isShutdown: false, isTerminated: false
 INFO [main] (LinkChecker.java:134) - Going to sleep until all tasks are complete
 INFO [Monitor] (TasksMonitorThread.java:24) - [monitor] [10/10] Active: 10, Completed: 68, Task: 236, isShutdown: false, isTerminated: false
 INFO [Monitor] (TasksMonitorThread.java:24) - [monitor] [10/10] Active: 10, Completed: 127, Task: 236, isShutdown: false, isTerminated: false
 INFO [Monitor] (TasksMonitorThread.java:24) - [monitor] [10/10] Active: 10, Completed: 179, Task: 236, isShutdown: false, isTerminated: false
 WARN [pool-1-thread-7] (ResponseProcessCookies.java:127) - Cookie rejected: "[version: 0][name: clasic][value: deleted][domain: http://anuncios.ebay.es][path: /][expiry: Wed May 19 16:40:51 PDT 2010]". Illegal domain attribute "http://anuncios.ebay.es". Domain of origin: "www.ebayanuncios.es"
 INFO [Monitor] (TasksMonitorThread.java:24) - [monitor] [10/10] Active: 3, Completed: 233, Task: 236, isShutdown: false, isTerminated: false
 INFO [Monitor] (TasksMonitorThread.java:24) - [monitor] [3/10] Active: 2, Completed: 234, Task: 236, isShutdown: false, isTerminated: false
 INFO [Monitor] (TasksMonitorThread.java:24) - [monitor] [2/10] Active: 2, Completed: 234, Task: 236, isShutdown: false, isTerminated: false
 INFO [Monitor] (TasksMonitorThread.java:24) - [monitor] [2/10] Active: 2, Completed: 234, Task: 236, isShutdown: false, isTerminated: false
 INFO [Monitor] (TasksMonitorThread.java:24) - [monitor] [2/10] Active: 0, Completed: 236, Task: 236, isShutdown: false, isTerminated: false
 INFO [Monitor] (TasksMonitorThread.java:33) - Call it done
 INFO [main] (LinkChecker.java:140) - Got notified
 INFO [main] (LinkChecker.java:142) - Stopped the pool: java.util.concurrent.ThreadPoolExecutor@47570945
 WARN [main] (LinkChecker.java:70) - Broken link: org.open.pagehealth.PageLink{_link='http://www.ebay.com/#mainContent', _caption='Skip to main content', _isGood=false, _responseStatus='HTTP/1.1 404 Not Found', _isVerified=true, _contentType='null', _contentLenght=0}
 WARN [main] (LinkChecker.java:70) - Broken link: org.open.pagehealth.PageLink{_link='', _caption='Hide eBay suggestions', _isGood=false, _responseStatus='Empty Link', _isVerified=true, _contentType='null', _contentLenght=0}
 WARN [main] (LinkChecker.java:70) - Broken link: org.open.pagehealth.PageLink{_link='', _caption='Start of layer', _isGood=false, _responseStatus='Empty Link', _isVerified=true, _contentType='null', _contentLenght=0}
 WARN [main] (LinkChecker.java:70) - Broken link: org.open.pagehealth.PageLink{_link='', _caption='Click to close', _isGood=false, _responseStatus='Empty Link', _isVerified=true, _contentType='null', _contentLenght=0}
 WARN [main] (LinkChecker.java:70) - Broken link: org.open.pagehealth.PageLink{_link='', _caption='End of layer', _isGood=false, _responseStatus='Empty Link', _isVerified=true, _contentType='null', _contentLenght=0}
 INFO [main] (LinkChecker.java:73) - Result: 231/236, health: 97.881355%
 INFO [main] (LinkChecker.java:54) - Total time 25119 ms
