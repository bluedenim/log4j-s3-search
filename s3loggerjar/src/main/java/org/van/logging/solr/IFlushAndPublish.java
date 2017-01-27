package org.van.logging.solr;

import java.util.concurrent.Future;

/**
 * An interface for an implementation that will flush and publish
 * cached content.
 */
public interface IFlushAndPublish {
    /**
     * Flush and publish cached content and return a {@link Future <Boolean>}
     * for the result.
     *
     * @return {@link Future<Boolean>} for the result. This CAN BE
     * <code>null</code> if there was nothing published.
     */
    Future<Boolean> flushAndPublish();
}
