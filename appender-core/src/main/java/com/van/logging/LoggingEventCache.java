package com.van.logging;

import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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

    private File tempBufferFile;
    private final Object bufferLock = new Object();
    private AtomicReference<ObjectOutputStream> objectOutputStreamRef =
        new AtomicReference<>();
    private AtomicInteger eventCount = new AtomicInteger();

    private final IBufferMonitor<T> cacheMonitor;
    private final IBufferPublisher<T> cachePublisher;
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

        synchronized(bufferLock) {
            tempBufferFile = File.createTempFile(this.cacheName, null);
            /*
            System.out.println(
                String.format("Creating temporary file for event cache: %s",
                    tempBufferFile));
           */
           this.objectOutputStreamRef.set(
               new ObjectOutputStream(new FileOutputStream(tempBufferFile)));
            this.eventCount.set(0);
        }

        executorService = createExecutorService();
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
            objectOutputStreamRef.get().writeObject(event);
            eventCount.incrementAndGet();
        }
        cacheMonitor.eventAdded(event, this);
    }

    /**
     * Publish the current staging log to remote stores if the staging log
     * is not empty.
     *
     * @return a Future &lt;Boolean&gt; representing the result of the flush
     * and publish operation. Caller can call {@link Future#get()} on it to
     * wait for the operation. NOTE: This value CAN BE null if there was nothing
     * to publish.
     */
    @Override
    public Future<Boolean> flushAndPublish() {
        Future<Boolean> f = null;
        if (eventCount.get() > 0) {
            f = publishCache(cacheName);
        }
        return f;
    }

    @SuppressWarnings("unchecked")
    Future<Boolean> publishCache(final String name) {
        return executorService.submit(() -> {
            boolean success = true;
            try {
                Thread.currentThread().setName(PUBLISH_THREAD_NAME);
                PublishContext context = cachePublisher.startPublish(cacheName);

                int count = 0;
                File tempFile = null;

                synchronized (bufferLock) {
                    objectOutputStreamRef.get().close();

                    tempFile = this.tempBufferFile;
                    count = this.eventCount.get();
                    tempBufferFile = File.createTempFile(this.cacheName, null);
                    /*
                    System.out.println(
                        String.format("Creating temporary file for event cache: %s",
                            tempBufferFile));
                    */
                    this.objectOutputStreamRef.set(
                        new ObjectOutputStream(new FileOutputStream(tempBufferFile)));
                    this.eventCount.set(0);
                }
                // System.out.println(String.format("Publishing from file: %s", tempFile));
                try (FileInputStream fis = new FileInputStream(tempFile);
                     ObjectInputStream ois = new ObjectInputStream(fis)) {
                    for (int i = 0; i < count; i++) {
                        cachePublisher.publish(context, count, (T) ois.readObject());
                    }
                    cachePublisher.endPublish(context);
                } finally {
                    try {
                        tempFile.delete();
                    } catch (Exception ex) {
                    }
                }
            } catch (Throwable t) {
                System.err.println(String.format("Error while publishing cache: %s", t.getMessage()));
                t.printStackTrace();
            }
            return success;
        });
    }
}

