package net.hyperic.sigar.win32;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * A simple thread that runs forever monitoring the event log.
 */
public class EventLogThread implements Runnable {

    public static final int DEFAULT_INTERVAL = 60 * 1000;

    private static Logger logger =
        Logger.getLogger(EventLogThread.class.getName());

    private Thread thread = null;
    private static EventLogThread instance = null;
    private boolean shouldDie = false;
    private Set notifiers     = Collections.synchronizedSet(new HashSet());

    private String logName = "Application";
    private long interval  = 10 * 1000; // Default to 10 seconds

    public static synchronized EventLogThread getInstance() {
        if (instance == null) {
            instance = new EventLogThread();
        }

        return instance;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public void setLogName(String logName) {
        this.logName = logName;
    }
    
    public synchronized void doStart() {
        if (this.thread != null) {
            return;
        }

        this.thread = new Thread(this, "EventLogThread");
        this.thread.setDaemon(true);
        this.thread.start();

        logger.debug(this.thread.getName() + " started");
    }

    public synchronized void doStop() {
        if (this.thread == null) {
            return;
        }
        die();
        this.thread.interrupt();
        logger.debug(this.thread.getName() + " stopped");
        this.thread = null;
    }

    public void add(EventLogNotification notifier) {
        this.notifiers.add(notifier);
    }

    public void remove(EventLogNotification notifier) {
        this.notifiers.remove(notifier);
    }

    private void handleEvents(EventLog log, int curEvent, int lastEvent)
    {
        for (int i = curEvent + 1; i <= lastEvent; i++) {

            EventLogRecord record;

            try {
                record = log.read(i);
            } catch (Win32Exception e) {
                logger.error("Unable to read event id " + i + ": " + e);
                continue;
            }

            synchronized (this.notifiers) {
                for (Iterator it = this.notifiers.iterator(); it.hasNext();)
                {
                    EventLogNotification notification =
                        (EventLogNotification)it.next();
                    if (notification.matches(record))
                        notification.handleNotification(record);
                }
            }
        }
    }

    public void run() {
        
        EventLog log = new EventLog();
        int curEvent;

        try {
            // Open the event log
            log.open(this.logName);

            curEvent = log.getNewestRecord();

            while (!shouldDie) {
                // XXX: Using the waitForChange() method would be a
                //      cleaner way to go, but we cannot interrupt
                //      a native system call.
                int lastEvent = log.getNewestRecord();
                if (lastEvent > curEvent) {
                    handleEvents(log, curEvent, lastEvent);
                }

                curEvent = lastEvent;

                try {
                    Thread.sleep(this.interval);
                } catch (InterruptedException e) {
                }
            }
            
            log.close();

        } catch (Win32Exception e) {
            logger.error("Unable to monitor event log:", e);
        } finally {
            try { log.close(); } 
            catch (Win32Exception e) {}}
    }

    public void die() {
        this.shouldDie = true;
    }
}