package org.van.logging;

import org.apache.log4j.spi.LoggingEvent;
import org.van.logging.solr.IFlushAndPublish;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of {@link IBufferMonitor} that flushes the cache when its capacity
 * reaches a limit.
 */
public class CapacityBasedBufferMonitor implements IBufferMonitor {

    private final int cacheLimit;
    private final AtomicInteger countRef = new AtomicInteger();

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
        this.cacheLimit = cacheLimit;
    }

    @Override
    public void eventAdded(final LoggingEvent event, final IFlushAndPublish cache) {
        int count = countRef.incrementAndGet();
        if (count >= cacheLimit) {
            try {
                cache.flushAndPublish();
            } finally {
                countRef.set(0);
            }
        }
    }

    @Override
    public String toString() {
        return String.format("CapacityBasedBufferMonitor(cacheLimit: %d)", cacheLimit);
    }
}
