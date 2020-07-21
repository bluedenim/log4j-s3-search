package com.van.logging;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * An event cache that buffers/collects events and publishes them in a
 * background thread when the buffer fills up.
 *
 * @author vly
 *
 */
public class LoggingEventCache<T> implements IFlushAndPublish {

    public static final String PUBLISH_THREAD_NAME =
        "LoggingEventCache-publish-thread";

    private static final String DEFAULT_TEMP_FILE_PREFIX = "log4j-s3";

    private final String cacheName;

    private final Object bufferLock = new Object();
    private AtomicReference<List<Object>> objectOutputStreamRef =
        new AtomicReference<>();
    private AtomicInteger eventCount = new AtomicInteger();

    private final IBufferMonitor<T> cacheMonitor;
    private final IBufferPublisher<T> cachePublisher;

    private BlockingQueue<List<Object>> eventQueue = new LinkedBlockingQueue<>(100);

    /**
     * Creates an instance with the provided buffer publishing collaborator.
     * The instance will create a buffer of the capacity specified and will
     * publish a batch when collected events reach that capacity.
     *
     * To keep memory footprint down, the cache of events collected will be
     * implemented in a temporary file instead of in memory.
     *
     * @param cacheName name for the buffer
     * @param cacheMonitor the monitor for the buffer that will determine when
     *                     to trigger the flushing and publishing of the cache.
     * @param cachePublisher the publishing collaborator used to perform the
     *                       actual publishing of collected events.
     *
     * @throws Exception if errors occurred during instantiation
     */
    public LoggingEventCache(String cacheName, IBufferMonitor<T> cacheMonitor,
                             IBufferPublisher<T> cachePublisher) throws Exception {
        if (null == cacheName) {
            this.cacheName = DEFAULT_TEMP_FILE_PREFIX;
        } else {
            this.cacheName = cacheName;
        }
        this.cacheMonitor = cacheMonitor;
        this.cachePublisher = cachePublisher;

        new Thread(new CacheConsumer(eventQueue)).start();
        
        synchronized(bufferLock) {
            System.out.println("Creating event cache");
           
           this.objectOutputStreamRef.set(new ArrayList<Object>());
           this.eventCount.set(0);
        }
    }

    ExecutorService createExecutorService() {
        return Executors.newFixedThreadPool(1);
    }

    /**
     * Retrieves the name of the cache
     *
     * @return name of the cache
     */
    public String getCacheName() {
        return cacheName;
    }

    /**
     * Adds a log event to the cache.  If the number of events reach the
     * capacity of the batch, they will be published.
     *
     * @param event the log event to add to the cache.
     *
     * @throws IOException if exceptions occurred while dealing with I/O
     */
    public void add(T event) throws IOException {
        synchronized (bufferLock) {
            objectOutputStreamRef.get().add(event);
            eventCount.incrementAndGet();
        }
        cacheMonitor.eventAdded(event, this);
    }

    /**
     * Publish the current staging log to remote stores if the staging log
     * is not empty.
     */
    @Override
    public void flushAndPublish() {
        if (eventCount.get() > 0) {
        	queueCache(cacheName);
        }
    }

    void queueCache(final String name) {
        synchronized (bufferLock) {
			try {
	        	//System.out.println("Creating clone from cache for producer queue");
				eventQueue.put(objectOutputStreamRef.get().stream().collect(Collectors.toList()));
			} catch (InterruptedException e) {
				System.err.println(String.format("Error while adding cache to queue : %s", e.getMessage()));
				e.printStackTrace();
			}
			objectOutputStreamRef.get().clear();
			this.eventCount.set(0);
        }
    }

	@SuppressWarnings("unchecked")
	private Boolean publishCache(List<Object> eventList) {
		PublishContext context = cachePublisher.startPublish(cacheName + System.currentTimeMillis());
		boolean success = true;
		try {
		    //System.out.println("Publishing cached event list");
		    for (int i = 0; i < eventList.size(); i++) {
	            cachePublisher.publish(context, eventList.size(), (T) eventList.get(i));
	        }
	        cachePublisher.endPublish(context);
		} catch (Throwable t) {
		    System.err.println(String.format("Error while publishing cache: %s", t.getMessage()));
		    t.printStackTrace();
		    success = false;
		}
		return success;
	}
    
    public class CacheConsumer implements Runnable {
        private BlockingQueue<List<Object>> queue;
        
        public CacheConsumer(BlockingQueue<List<Object>> queue) {
            this.queue = queue;
        }
        public void run() {
        	while (true) {
        		try {
                	publishCache(queue.take());
                } catch (Throwable t) {
                	t.printStackTrace();
                }
            }
        }
    }
}

