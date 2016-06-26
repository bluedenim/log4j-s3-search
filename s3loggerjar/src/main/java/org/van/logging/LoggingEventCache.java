package org.van.logging;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.spi.LoggingEvent;

/**
 * An event cache that buffers/collects events and publishes them in a 
 * background thread when the buffer fills up.
 * 
 * @author vly
 *
 */
public class LoggingEventCache {
	
	public static final String PUBLISH_THREAD_NAME =
		"LoggingEventCache-publish-thread";
	
	/**
	 * Interface for a publishing collaborator 
	 * 
	 * @author vly
	 *
	 */
	public interface ICachePublisher {
		
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
			final LoggingEvent event);
		
		/**
		 * Concludes a publish batch.  Implementations should submit/commit
		 * a batch and/or clean up resources in preparation for the next
		 * batch.
		 * 
		 * @param context the context for this batch
		 */
		void endPublish(final PublishContext context);
	}

	private final String cacheName;
	private final int capacity;
	
	private final Object EVENTQUEUELOCK = new Object();
	// Supposedly Log4j already takes care of concurrency for us, so theoretically
	// we do not need the EVENTQUEUELOCK around {eventQueue, eventQueueLength}
	// (or the need to use a ConcurrentLinkedQueue as opposed to just a normal
	// List).  Dunno.  To be safe, I am using them.
	private Queue<LoggingEvent> eventQueue =			
		new ConcurrentLinkedQueue<LoggingEvent>();
	private volatile int eventQueueLength = 0;
	
	private final ICachePublisher cachePublisher;
	private final ExecutorService executorService;
	
	/**
	 * Creates an instance with the provided cache publishing collaborator.
	 * The instance will create a buffer of the capacity specified and will
	 * publish a batch when collected events reach that capacity.
	 * 
	 * @param cacheName name for the cache
	 * @param capacity the capacity of the buffer for events before the buffer
	 * is published
	 * @param cachePublisher the publishing collaborator
	 */
	public LoggingEventCache(String cacheName, int capacity, 
		ICachePublisher cachePublisher) {
		this.cacheName = cacheName;
		this.capacity = capacity;
		this.cachePublisher = cachePublisher;
		executorService = createExecutorService(); 
	}
	
	ExecutorService createExecutorService() {
		ExecutorService svc = Executors.newFixedThreadPool(1);
		return svc;
	}		
	
	/**
	 * Retrieves the name of the cache
	 * 
	 * @return
	 */
	public String getCacheName() {
		return cacheName;
	}
	
	/**
	 * Adds a log event to the cache.  If the number of events reach the
	 * capacity of the batch, they will be published.
	 * 
	 * @param event the log event to add to the cache.
	 */
	public void add(LoggingEvent event) {
		boolean publish = false;
		synchronized(EVENTQUEUELOCK) {
			if (eventQueueLength < capacity) {
				eventQueue.add(event);
				eventQueueLength++;
			} else {
				publish = true; 
			}
		}
		if (publish) {
			flushAndPublishQueue(false);
		}
	}
	
	/**
	 * Publish the current staging log to remote stores if the staging log
	 * is not empty.
	 * 
	 */
	public void flushAndPublishQueue(boolean block) {
		Queue<LoggingEvent> queueToPublish = null;
		synchronized(EVENTQUEUELOCK) {
			if (eventQueueLength > 0) {
				queueToPublish = eventQueue;
				eventQueue = new ConcurrentLinkedQueue<LoggingEvent>();
				eventQueueLength = 0;
			}
		}
		if (null != queueToPublish) {
			Future<Boolean> f = publishCache(cacheName, queueToPublish);
			if (block) {
				try {
					f.get();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	Future<Boolean> publishCache(final String name, final Queue<LoggingEvent> eventsToPublish) {
		Future<Boolean> f = executorService.submit(new Callable<Boolean>() {
			public Boolean call() {
				Thread.currentThread().setName(PUBLISH_THREAD_NAME);
				int sequence = 0;
				PublishContext context = cachePublisher.startPublish(cacheName);
				for (LoggingEvent evt: eventsToPublish) {
					cachePublisher.publish(context, sequence, evt);
					sequence++;
				}
				cachePublisher.endPublish(context);
				return true;
			}
		});
		return f;
	}
}
