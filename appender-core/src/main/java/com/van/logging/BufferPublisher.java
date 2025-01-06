package com.van.logging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation to standardize on a cache name and aggregate and coordinate
 * multiple IPublishHelpers to publish content to different destinations.
 *
 * @author vly
 *
 */
public class BufferPublisher implements IBufferPublisher {
    private final String hostName;
    private final String[] tags;
    private final boolean excludeHostInLogName;

    private List<IPublishHelper> helpers =
        new LinkedList<>();

    public BufferPublisher(String hostName, String[] tags) {
        this(hostName, tags, false);
    }

    /**
     * Creates a new instance of the publisher.
     *
     * @param hostName - the host name (to be used as part of the cache name)
     * @param tags - additional tags to include when publishing to search engines
     * @param excludeHostInLogName - if False, the host name will not be included in published cache name
     */
    public BufferPublisher(String hostName, String[] tags, boolean excludeHostInLogName) {
        this.hostName = hostName;
        this.tags = tags;
        this.excludeHostInLogName = excludeHostInLogName;
    }

    public PublishContext startPublish(String cacheName) {
        String namespacedCacheName = composeNamespacedCacheName(cacheName);
		if (VansLogger.logger.isDebugEnabled()) {
            VansLogger.logger.debug(
                String.format(
                    "BEGIN publishing %s. excludeHostInLogName is %s...",
                    namespacedCacheName, excludeHostInLogName
                )
            );
        }
        PublishContext context = new PublishContext(namespacedCacheName,
            hostName, tags);
        for (IPublishHelper helper: helpers) {
            try {
                helper.start(context);
            } catch (Throwable t) {
                VansLogger.logger.error(
                    String.format("Cannot start publish with %s due to error", helper), t
                );
            }
        }
        return context;
    }

    String composeNamespacedCacheName(String rawCacheName) {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        if (this.excludeHostInLogName) {
            return String.format("%s_%s", df.format(new Date()), rawCacheName);
        } else {
            return String.format("%s_%s_%s", df.format(new Date()),
                hostName, rawCacheName);
        }
    }

    public void publish(PublishContext context, int sequence, Event event) {
        for (IPublishHelper helper: helpers) {
            try {
                helper.publish(context, sequence, event);
            } catch (Throwable t) {
                VansLogger.logger.error(
                    String.format("Cannot publish with %s due to error", helper), t
                );
            }
        }
    }

    public void endPublish(PublishContext context) {
        for (IPublishHelper helper: helpers) {
            try {
                helper.end(context);
            } catch (Throwable t) {
                VansLogger.logger.error(
                    String.format("Cannot end publish with %s due to error", helper), t
                );
            }
        }
        if (VansLogger.logger.isDebugEnabled()) {
            VansLogger.logger.debug(String.format("END publishing %s", context.getCacheName()));
        }
    }

    /**
     * Add an IPublishHelper implementation to the list of helpers to invoke
     * when publishing is performed.
     *
     * @param helper helper to add to the list
     */
    public void addHelper(IPublishHelper helper) {
        helpers.add(helper);
    }
}
