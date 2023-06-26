package com.van.logging;

/**
 * Implementation of {@link IBufferMonitor} that flushes the cache when its capacity
 * reaches a limit.
 */
public class CapacityBasedBufferMonitor implements IBufferMonitor {

    private final int cacheLimit;
    private final Object countGuard = new Object();
    private int count = 0;
    private boolean verbose = false;

    /**
     * Creates an instance with the cache limit as provided. When hooked up to a
     * LoggingEventCache, this object will flush the cache when the cache's event
     * count reaches the limit.
     *
     * @param cacheLimit the limit in number of event messages. When the associated
     *                   {@link LoggingEventCache}'s event cache size reaches this
     *                   limit, the event cache will be flushed and published.
     */
    public CapacityBasedBufferMonitor(int cacheLimit) {
        this(cacheLimit, false);
    }

    public CapacityBasedBufferMonitor(int cacheLimit, boolean verbose) {
        this.cacheLimit = cacheLimit;
        this.verbose = verbose;
    }

    @Override
    public void eventAdded(final Event event, final IFlushAndPublish cache) {
        boolean flush = false;
        synchronized (countGuard) {
            if (++count >= cacheLimit) {
                flush = true;
                count = 0;
            }
        }
        if (flush) {
            try {
                cache.flushAndPublish();
            } catch (Exception ex) {
                VansLogger.logger.error("Cannot flush and publish", ex);
            }
        }
    }

    @Override
    public void shutDown() {
        if (this.verbose) {
            VansLogger.logger.info("CapacityBasedBufferMonitor: shutting down.");
        }
    }

    @Override
    public String toString() {
        return String.format("CapacityBasedBufferMonitor(cacheLimit: %d)", cacheLimit);
    }
}
