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
public class BufferPublisher<T> implements IBufferPublisher<T> {
    private final String hostName;
    private final String[] tags;

    private List<IPublishHelper<T>> helpers =
        new LinkedList<>();

    public BufferPublisher(String hostName, String[] tags) {
        this.hostName = hostName;
        this.tags = tags;
    }

    public PublishContext startPublish(String cacheName) {
        String namespacedCacheName = composeNamespacedCacheName(cacheName);
		/* System.out.println(String.format("BEGIN publishing %s...",
			namespacedCacheName)); */
        PublishContext context = new PublishContext(namespacedCacheName,
            hostName, tags);
        for (IPublishHelper helper: helpers) {
            try {
                helper.start(context);
            } catch (Throwable t) {
                System.err.println(String.format("Cannot start publish with %s due to error: %s",
                    helper, t.getMessage()));
            }
        }
        return context;
    }

    String composeNamespacedCacheName(String rawCacheName) {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        return String.format("%s_%s_%s", df.format(new Date()),
            hostName, rawCacheName);
    }

    public void publish(PublishContext context, int sequence, T event) {
        for (IPublishHelper<T> helper: helpers) {
            try {
                helper.publish(context, sequence, event);
            } catch (Throwable t) {
                System.err.println(String.format("Cannot publish with %s due to error: %s",
                    helper, t.getMessage()));
            }
        }
        /* System.out.println(String.format("%d:%s", sequence,
			layout.format(event))); */
    }

    public void endPublish(PublishContext context) {
        for (IPublishHelper helper: helpers) {
            try {
                helper.end(context);
            } catch (Throwable t) {
                t.printStackTrace(System.err);
                System.err.println(String.format("Cannot end publish with %s due to error: %s",
                    helper, t.getMessage()));
            }
        }
		/* System.out.println(String.format("END publishing %s",
			context.getCacheName())); */
    }

    /**
     * Add an IPublishHelper implementation to the list of helpers to invoke
     * when publishing is performed.
     *
     * @param helper helper to add to the list
     */
    public void addHelper(IPublishHelper<T> helper) {
        helpers.add(helper);
    }
}
