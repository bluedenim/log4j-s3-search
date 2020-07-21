package com.van.logging;


/**
 * An interface for an implementation that will flush and publish
 * cached content.
 */
public interface IFlushAndPublish {
    /**
     * Flush and publish cached content and return a Future &lt;Boolean&gt;
     * for the result.
     */
    void flushAndPublish();
}
