package org.van.logging.log4j;

import org.apache.log4j.spi.LoggingEvent;
import org.van.logging.PublishContext;

/**
 * Interface for publish implementations to actually push log events to
 * external stores.
 * 
 * @author vly
 *
 */
public interface IPublishHelper {
	/**
	 * A publish batch is starting.  This is a good place to (re)initialize
	 * a buffer.
	 * 
	 * @param context publish context providing useful properties for the
	 * publish operation
	 */
	void start(PublishContext context);
	
	/**
	 * A log event should be published.  Implementations may want to accumulate
	 * this in a batch until {{@link #end(PublishContext)}
	 * 
	 * @param context publish context providing useful properties for the
	 * publish operation
	 * @param sequence counter of the sequence of this event in the batch
	 * @param event the log event
	 */
	void publish(PublishContext context, int sequence, LoggingEvent event);
	
	/**
	 * A publish batch has ended.  Implementations should conclude a batch
	 * and clean up resources here.
	 * 
	 * @param context publish context providing useful properties for the
	 * publish operation
	 */
	void end(PublishContext context);
}
