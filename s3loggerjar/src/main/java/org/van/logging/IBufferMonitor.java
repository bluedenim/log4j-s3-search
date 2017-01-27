package org.van.logging;

import org.apache.log4j.spi.LoggingEvent;
import org.van.logging.solr.IFlushAndPublish;

/**
 * Monitors a {@link IFlushAndPublish} and flushes the buffer depending on
 * the implemented policy. The {@link IFlushAndPublish#flushAndPublish()}
 * can be used by implementations to flush the queue.
 */
public interface IBufferMonitor {
    /**
     * A log event has just been added to the event buffer. Handlers
     * can decide to flush the buffer if the conditions are right.
     *
     * @param event the {@link LoggingEvent} just added to the buffer.
     * @param flushAndPublisher the {@link IFlushAndPublish} to use to publish.
     */
    void eventAdded(final LoggingEvent event, final IFlushAndPublish flushAndPublisher);
}
