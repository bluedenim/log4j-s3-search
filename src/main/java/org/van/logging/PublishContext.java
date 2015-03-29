package org.van.logging;

import org.apache.log4j.Layout;

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
	private final Layout layout;
	
	/**
	 * Creates an instance with the data provided
	 * 
	 * @param cacheName name of the cache used to distinguish it from other
	 * 	caches
	 * @param hostName the host name where the logs are collected (typically
	 * 	the name of the local host)
	 * @param tags additional tags for the event that the logger was intialized
	 * 	with
	 * @param layout the Log4j layout used for the logger
	 */
	public PublishContext(String cacheName, String hostName, String[] tags, Layout layout) {
		this.cacheName = cacheName;
		this.hostName = hostName;
		this.tags = tags;
		this.layout = layout;
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

	public Layout getLayout() {
		return layout;
	}
		
}