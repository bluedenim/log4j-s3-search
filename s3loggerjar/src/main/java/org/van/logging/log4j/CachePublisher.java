package org.van.logging.log4j;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;
import org.van.logging.LoggingEventCache.ICachePublisher;
import org.van.logging.PublishContext;

/**
 * Implementation to standardize on a cache name and aggregate and coordinate
 * multiple IPublishHelpers to publish content to different destinations.
 * 
 * @author vly
 *
 */
public class CachePublisher implements ICachePublisher {
	private final Layout layout;
	private final String hostName;
	private final String[] tags;
	
	private List<IPublishHelper> helpers = 
		new LinkedList<>();
	
	public CachePublisher(Layout layout, String hostName, String[] tags) {
		this.layout = layout;	
		this.hostName = hostName;
		this.tags = tags;
	}
	
	public PublishContext startPublish(String cacheName) {
		String namespacedCacheName = composeNamespacedCacheName(cacheName);
		System.out.println(String.format("BEGIN publishing %s...",
			namespacedCacheName));
		PublishContext context = new PublishContext(namespacedCacheName,
			hostName, tags, layout);
		for (IPublishHelper helper: helpers) {
			try {
				helper.start(context);
			} catch (Throwable t) {
				System.out.println(String.format("Cannot start publish with %s due to error: %s",
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

	public void publish(PublishContext context, int sequence, LoggingEvent event) {
		for (IPublishHelper helper: helpers) {
			try {
				helper.publish(context, sequence, event);
			} catch (Throwable t) {
				System.out.println(String.format("Cannot publish with %s due to error: %s",
					helper, t.getMessage()));
			}
		}
//		System.out.println(String.format("%d:%s", sequence,
//			layout.format(event)));
	}

	public void endPublish(PublishContext context) {
		for (IPublishHelper helper: helpers) {
			try {
				helper.end(context);
			} catch (Throwable t) {
				System.out.println(String.format("Cannot end publish with %s due to error: %s",
					helper, t.getMessage()));
			}
		}
		System.out.println(String.format("END publishing %s",
			context.getCacheName()));
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
