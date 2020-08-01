package com.van.logging;

import java.util.concurrent.Future;

/**
 * An interface for an implementation that will flush and publish
 * cached content.
 */
public interface IFlushAndPublish {
    /**
     * Flush and publish cached content and return a Future &lt;Boolean&gt;
     * for the result.
     *
     * @return Future &lt;Boolean&gt; for the result. This CAN BE
     * <code>null</code> if there was nothing published.
     */
    default Future<Boolean> flushAndPublish() {
        return this.flushAndPublish(false);
    }

    /**
     * Flush and publish cached content and return a Future &lt;Boolean&gt;
     * for the result.
     *
     * @param useCurrentThread: if True, publish using the current thread instead of a background thread. This should
     *     not be true in normal cases. One case where it should be true is when called from the shutdown hook.
     *
     * @return Future &lt;Boolean&gt; for the result. This CAN BE
     * <code>null</code> if there was nothing published.
     */
    public Future<Boolean> flushAndPublish(boolean useCurrentThread);
}
