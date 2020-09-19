package com.van.logging;

/**
 * Monitors a {@link IFlushAndPublish} and flushes the buffer depending on
 * the implemented policy. The {@link IFlushAndPublish#flushAndPublish()}
 * can be used by implementations to flush the queue.
 */
public interface IBufferMonitor<T> {
    /**
     * A log event has just been added to the event buffer. Handlers
     * can decide to flush the buffer if the conditions are right.
     *
     * @param event the event just added to the buffer.
     * @param flushAndPublisher the {@link IFlushAndPublish} to use to publish.
     */
    void eventAdded(final T event, final IFlushAndPublish flushAndPublisher);

    /**
     * Shut down the monitor and clean up
     */
    void shutDown();
}
