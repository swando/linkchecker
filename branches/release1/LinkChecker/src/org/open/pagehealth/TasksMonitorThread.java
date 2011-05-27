package org.open.pagehealth;

import org.apache.log4j.Logger;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * My monitor thread. To monitor the status of {@link ThreadPoolExecutor}
 * and its status.
 */
public class TasksMonitorThread implements Runnable {
    private static final Logger LOG = Logger.getLogger(TasksMonitorThread.class);
    ThreadPoolExecutor executor;
    Object             _callBack;

    public TasksMonitorThread(final ThreadPoolExecutor executor, final Object callBack) {
        this.executor = executor;
        this._callBack = callBack;
    }

    public void run() {
        try {
            do {
                LOG.info(String.format(
                        "[monitor] ActiveThreads: %d, TotalThreads: %d, CompletedTasks: %d, TotalTasks: %d",
                        this.executor.getActiveCount(), this.executor.getCorePoolSize(),
                        this.executor.getCompletedTaskCount(), this.executor.getTaskCount()));

                if (this.executor.getTaskCount() > 0 && this.executor.getActiveCount() == 0 &&
                        this.executor.getTaskCount() == this.executor.getCompletedTaskCount()) {
                    LOG.debug("Task queue is empty. Lets shutdown.");
                    synchronized (_callBack) {
                        _callBack.notify();
                    }
                    return;
                }

                Thread.sleep(3000);
            } while (true);
        } catch (Exception e) {
            LOG.error("Exception", e);
        }
    }
}
