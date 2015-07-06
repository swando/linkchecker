
# Introduction #

  * Project Website: http://code.google.com/p/linkchecker/
  * Source Code: http://code.google.com/p/linkchecker/source/checkout
  * Download: http://code.google.com/p/linkchecker/downloads/list
  * Sample Report: http://linkchecker.googlecode.com/files/sample-report.html

# Description #

This is a java based command line application.

LinkChecker application accepts a website page URL as input. This page is then scanned for all the elements which points
to other urls, images, files etc. A collection of all Links are then scanned further and classified into 'good' and
'broken' links. Serial scanning 100s of such URLs may take time and hence a thread pool is introduced to run the scan
in parallel.
A nice HTML report ([sample](http://linkchecker.googlecode.com/files/sample-report.html)) is generated and launched at
the end with all the details of all the URLs and other stats.

  * Using Java Version 1.6.0\_24 for development and testing.
  * Application is tested on MAC and PC.
  * Ant build file is supplied to build application elsewhere. Use command 'ant clean build' to generate tar.qz file.
  * Application is developed using IntelliJ IDEA 10.3 IDE and sources contains the relevant project files.
  * Application uses log4j for logging and logging configuration can be controlled via file - linkchecker.properties
  * JLine is used for interacting on console.
  * JSoup is used for scanning the root website for retrieving all elements (links, images and other assets)
  * HttpClient is used for checking the URLs via GET/HEAD requests
  * XML and XSLT is used for presentation layer ([sample](http://linkchecker.googlecode.com/files/sample-report.html))

# Future features may include #
  * Full website scan and health report ([sample](http://linkchecker.googlecode.com/files/sample-report.html))
  * Support for cookies to deliver browser-like experience
  * Support for HTTP authentication like Basic and Digest
  * Better support for HTTPS handling
  * Further deep classification based on URLs
  * Support for Flash
  * Interactive command line to change the application behaviour
  * Convert this standalone application to a web service and execute it on server (very tempting for me).
  * Extend the support for validating javascript links.
  * A enhanced GUI to report the status while the scanning is in progress. Currently we see the progress pending/total only on the command line.


# How to run #

  1. Download the latest application tar.qz file from http://code.google.com/p/linkchecker/downloads/list
  1. Extract the file using 'tar -xvf linkchecker.tar.gz' into folder 'linkchecker'
  1. cd into extracted location using 'cd linkchecker'
  1. Make sure machine is set with right java version and 'java' is in the path.
  1. Launch the LinkChecker application using following command
> > java -jar linkchecker.jar
  1. Enter the website URL when prompted and hit Return
  1. Wait for the application to scan all the links and look for the report file: report.html ([sample](http://linkchecker.googlecode.com/files/sample-report.html)) Application makes an attempt to launch this report file at the end of each scan. Click on the column names in the tables to sort.


# Application Properties #

Application properties are stored in a file called linkchecker.properties. This file is shipped with tar.gz and can
be found in the extracted folder.

Some of the important properties are as follows...
## Deep scan Vs Quick Scan ##
By default all the URLs are scanned using HTTP GET which is somewhat expensive operation since it downloads the whole
page (HTTP response payload entity). The validity of the URL can even be checked via HTTP HEAD request. This is
similar to HTTP GET but does not download the webpage (asset). The switch to this efficient mode,
just add following property to linkchecker.properties file.

> org.open.pagehealth.quick-check-head=true

## Pool Size ##
A thread pool created to scan the URLs. The number of threads spawned by pool can be controlled via following property
> org.open.pagehealth.poolsize=10

## Timeout ##
Some of the websites are slow OR may contain heavy assets which may take longer times to validate the URLs.
The timout can be configured via following property
> org.open.pagehealth.timeout=10000