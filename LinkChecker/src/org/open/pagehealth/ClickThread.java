package org.open.pagehealth;

import org.apache.log4j.Logger;

/**
 * User: swapnilsapar
 * Date: 5/3/11
 * Time: 1:27 PM
 */
public class ClickThread implements Runnable {
    private static final Logger LOG = Logger.getLogger(ClickThread.class);
    private PageLink _Page_link;
    private String         _name;

    public ClickThread(String name, PageLink pageLink) {
        _name = name;
        _Page_link = pageLink;
    }

    public void run() {
        LOG.debug(this._name + " : I'm running ! ");
        _Page_link.checkLink();
        LOG.debug(this._name + " : I'm done ! ");
    }
}
