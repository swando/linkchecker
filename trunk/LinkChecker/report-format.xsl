<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:template match="/">
        <html>
            <head>
                <script src="sorttable.js"></script>
            </head>
            <body>
                <h1>
                    <a name="top">LinkChecker Report</a>
                </h1>
                <h2>
                    <a name="index">Index</a>
                </h2>
                <ul>
                    <li>
                        <a href="#index">Index</a>
                    </li>
                    <li>
                        <a href="#summary">Summary</a>
                    </li>
                    <li>
                        <a href="#pool">Thread-Pool Details</a>
                    </li>
                    <li>
                        <a href="#url">URL Details</a>
                    </li>
                </ul>

                <h2>
                    <a name="summary">Summary</a>
                </h2>
                <ul>
                    <li>Scanned Page:
                        <a>
                            <xsl:attribute name="href">
                                <xsl:value-of select="result/page/summary/root-url"/>
                            </xsl:attribute>
                            <xsl:attribute name="target">_blank</xsl:attribute>
                            <xsl:value-of select="result/page/summary/root-url"/>
                        </a>
                    </li>
                    <li>Page Health:
                        <xsl:value-of select="format-number(result/page/summary/health, '##.#')"/> %
                    </li>
                    <li>Page Link Count:
                        <xsl:value-of select="result/page/summary/link-count"/>
                    </li>
                    <li>Total Threads:
                        <xsl:value-of select="result/page/summary/thread-count"/>
                    </li>
                    <li>Scan Duration:
                        <xsl:value-of select="format-number(result/page/summary/scan-time, '#,###')"/> ms
                    </li>
                    <li>Start Time:
                        <xsl:value-of select="result/page/summary/start-time"/>
                    </li>
                </ul>
                <div align="right">
                    <a href="#top">Top</a>
                </div>

                <h2>
                    <a name="pool">Thread-Pool Details</a>
                </h2>
                <table border="1" class="sortable">
                    <tr bgcolor="#9acd32">
                        <th>Name</th>
                        <th>Jobs Executed</th>
                    </tr>
                    <xsl:for-each select="result/page/thread-pool/threads/thread">
                        <tr>
                            <td>
                                <xsl:value-of select="name"/>
                            </td>
                            <td width="70%" style="text-align:left;">
                                <div>
                                    <xsl:attribute name="style">
                                        background-color:#FF9900; width: <xsl:value-of select="job-share"/>%;
                                    </xsl:attribute>
                                    <xsl:value-of select="job-count"/>
                                </div>

                            </td>
                        </tr>
                    </xsl:for-each>
                </table>
                <div align="right">
                    <a href="#top">Top</a>
                </div>

                <h2>
                    <a name="url">URL Details</a>
                </h2>
                <table border="1" class="sortable">
                    <tr bgcolor="#9acd32">
                        <th>Link</th>
                        <th>Caption</th>
                        <th>Type</th>
                        <th>Status</th>
                        <th>Verified</th>
                        <th>HTTP Response</th>
                        <th>Content-Type</th>
                        <th>Content-Length (bytes)</th>
                        <th>Scan Duration (ms)</th>
                        <th>Verified-By</th>
                    </tr>
                    <xsl:for-each select="result/page/links/link">
                        <tr>

                            <xsl:if test="status='Broken'">
                                <xsl:attribute name="style">
                                    background-color:#FF8566;
                                </xsl:attribute>
                            </xsl:if>
                            <td>
                                <a>
                                    <xsl:attribute name="href">
                                        <xsl:value-of select="url"/>
                                    </xsl:attribute>
                                    <xsl:attribute name="target">_blank</xsl:attribute>
                                    <xsl:value-of select="short-url"/>
                                </a>
                            </td>
                            <td>
                                <xsl:value-of select="caption"/>
                            </td>
                            <td>
                                <xsl:value-of select="type"/>
                            </td>
                            <td>
                                <xsl:value-of select="status"/>
                            </td>
                            <td>
                                <xsl:value-of select="verified"/>
                            </td>
                            <td>
                                <xsl:value-of select="http-response"/>
                            </td>
                            <td>
                                <xsl:value-of select="content-type"/>
                            </td>
                            <td>
                                <xsl:value-of select="content-length"/>
                            </td>
                            <td>
                                <xsl:value-of select="scan-time"/>
                            </td>
                            <td>
                                <xsl:value-of select="verified-thread"/>
                            </td>
                        </tr>
                    </xsl:for-each>
                </table>
                <div align="right">
                    <a href="#top">Top</a>
                </div>
            </body>
        </html>
    </xsl:template>

</xsl:stylesheet>
