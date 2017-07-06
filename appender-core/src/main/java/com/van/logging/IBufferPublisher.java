package com.van.logging;

/**
 * Interface for a publishing collaborator
 *
 * @author vly
 *
 */
public interface IBufferPublisher<T> {

    /**
     * Start a batch of events with the given name.  Implementations should
     * initialize a publish context and return it.
     *
     * @param cacheName the name for the batch of events
     *
     * @return a context for subsequent operations
     */
    PublishContext startPublish(final String cacheName);

    /**
     * Publish an event in the batch
     *
     * @param context the context for this batch
     * @param sequence the sequence of this event in the batch.  This
     * number increases per event
     * @param event the logging event
     */
    void publish(final PublishContext context, final int sequence,
                 final T event);

    /**
     * Concludes a publish batch.  Implementations should submit/commit
     * a batch and/or clean up resources in preparation for the next
     * batch.
     *
     * @param context the context for this batch
     */
    void endPublish(final PublishContext context);
}

