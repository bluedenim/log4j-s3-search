package com.van.logging;

/**
 * The context for a publish batch.  This object contains various auxiliary
 * information about the environment and configuration that publishers may
 * find useful.
 *
 * @author vly
 *
 */
public class PublishContext {
    private final String cacheName;
    private final String hostName;
    private final String[] tags;

    /**
     * Creates an instance with the data provided
     *
     * @param cacheName name of the cache used to distinguish it from other caches
     * @param hostName the host name where the logs are collected (typically
     * 	the name of the local host)
     * @param tags additional tags for the event that the logger was initialized with
     */
    public PublishContext(String cacheName, String hostName, String[] tags) {
        this.cacheName = cacheName;
        this.hostName = hostName;
        this.tags = tags;
    }

    public String getCacheName() {
        return cacheName;
    }

    public String getHostName() {
        return hostName;
    }

    public String[] getTags() {
        return tags;
    }

}
