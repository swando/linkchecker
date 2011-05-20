package org.open.pagehealth;

import org.apache.log4j.Logger;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * The custom {@link java.util.concurrent.RejectedExecutionHandler} to handle the rejected
 * tasks / {@link Runnable}
 */
public class TaskOverflowHandler implements RejectedExecutionHandler {
    private static final Logger LOG = Logger.getLogger(TaskOverflowHandler.class);

    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
        LOG.error(runnable.toString() + " : I've been rejected ! ");
    }
}
