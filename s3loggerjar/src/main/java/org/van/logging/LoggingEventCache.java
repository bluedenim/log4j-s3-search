package org.van.logging;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.spi.LoggingEvent;
import org.van.logging.solr.IFlushAndPublish;

/**
 * An event cache that buffers/collects events and publishes them in a 
 * background thread when the buffer fills up.
 * 
 * @author vly
 *
 */
public class LoggingEventCache implements IFlushAndPublish {
	
	public static final String PUBLISH_THREAD_NAME =
		"LoggingEventCache-publish-thread";

	private final String cacheName;

	private AtomicReference<Queue<LoggingEvent>> eventQueueRef =
		new AtomicReference<>(new ConcurrentLinkedQueue<LoggingEvent>());

	private final IBufferMonitor cacheMonitor;
	private final IBufferPublisher cachePublisher;
	private final ExecutorService executorService;
	
	/**
	 * Creates an instance with the provided buffer publishing collaborator.
	 * The instance will create a buffer of the capacity specified and will
	 * publish a batch when collected events reach that capacity.
	 * 
	 * @param cacheName name for the buffer
	 * @param cacheMonitor the monitor for the buffer that will determine when
	 *                     and effect the flushing and publishing of the cache.
	 * is published
	 * @param cachePublisher the publishing collaborator
	 */
	public LoggingEventCache(String cacheName, IBufferMonitor cacheMonitor,
		IBufferPublisher cachePublisher) {
		this.cacheName = cacheName;
		this.cacheMonitor = cacheMonitor;
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
        eventQueueRef.get().add(event);
        cacheMonitor.eventAdded(event, this);
	}
	
	/**
	 * Publish the current staging log to remote stores if the staging log
	 * is not empty.
	 *
     * @return a {@link Future<Boolean>} representing the result of the flush
     * and publish operation. Caller can call {@link Future#get()} on it to
     * wait for the operation. NOTE: This value CAN BE null if there was nothing
     * to publish.
	 */
	@Override
	public Future<Boolean> flushAndPublish() {
		Queue<LoggingEvent> queueToPublish = null;
        Future<Boolean> f = null;
        queueToPublish = eventQueueRef.getAndSet(new ConcurrentLinkedQueue<LoggingEvent>());
		// Do not use queueToPublish.size() since that is O(n)
		if ((null != queueToPublish) && !queueToPublish.isEmpty()) {
			f = publishCache(cacheName, queueToPublish);
		}
		return f;
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
