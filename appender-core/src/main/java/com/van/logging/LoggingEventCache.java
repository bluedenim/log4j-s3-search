package com.van.logging;

import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An event cache that buffers/collects events and publishes them in a
 * background thread when the buffer fills up.
 *
 * Implementation notes:
 *
 * The publishing of buffered events is done in another publishing thread (provided by this.executorService).
 * Depending on the incoming log event rates and the buffer size, the publishing may be overwhelmed if log events
 * are added faster than they can be published.
 *
 * Things that can be tweaked:
 *   - Buffer size or time setting for the buffer monitors (increasing the buffer size or time period will allow
 *     the buffer to store more events between publishes, giving more time for the publisher).
 *   - Increasing the number of threads (PUBLISHING_THREADS) for this.executorService.
 *
 * @author vly
 *
 */
public class LoggingEventCache<T> implements IFlushAndPublish {

    public static final String PUBLISH_THREAD_NAME =
        "LoggingEventCache-publish-thread";

    // Do not increase beyond 1 until all implementations of IBufferPublisher are thread safe or a new instance
    // of IBufferPublisher is used for each publish operation.
    private static final int PUBLISHING_THREADS = 1;

    private static final String DEFAULT_TEMP_FILE_PREFIX = "log4j-s3";

    private final String cacheName;

    private File tempBufferFile;
    private final Object bufferLock = new Object();
    private final AtomicReference<ObjectOutputStream> objectOutputStreamRef = new AtomicReference<>();
    private final AtomicInteger eventCount = new AtomicInteger();

    private final IBufferMonitor<T> cacheMonitor;
    private final IBufferPublisher<T> cachePublisher;
    private final ExecutorService executorService;

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
        return Executors.newFixedThreadPool(PUBLISHING_THREADS);
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

    @Override
    public Future<Boolean> flushAndPublish(boolean useCurrentThread) {
        Future<Boolean> f = null;
        if (eventCount.get() > 0) {
            f = publishCache(cacheName, useCurrentThread);
        }
        return f;
    }

    @SuppressWarnings("unchecked")
    void publishEventsFromFile(final AtomicReference<File> fileToPublishRef, final AtomicInteger eventCount) {
        try {
            PublishContext context = cachePublisher.startPublish(cacheName);
            File fileToPublish = fileToPublishRef.get();

            // System.out.println(String.format("Publishing from file: %s", tempFile));
            try (FileInputStream fis = new FileInputStream(fileToPublish);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                for (int i = 0; i < eventCount.get(); i++) {
                    cachePublisher.publish(context, i, (T) ois.readObject());
                }
                cachePublisher.endPublish(context);
            } finally {
                try {
                    fileToPublish.delete();
                } catch (Exception ex) {
                }
            }
        } catch (Throwable t) {
            System.err.println(
                String.format("Error while publishing cache from publishing thread: %s", t.getMessage())
            );
            t.printStackTrace();
        }
    }

    Future<Boolean> publishCache(final String name, final boolean useCurrentThread) {
        final AtomicReference<File> fileToPublishRef = new AtomicReference<>();
        final AtomicInteger eventCountInPublishFile = new AtomicInteger();
        boolean success = true;
        try {
            synchronized (bufferLock) {
                objectOutputStreamRef.get().close();

                fileToPublishRef.set(tempBufferFile);
                eventCountInPublishFile.set(eventCount.get());

                tempBufferFile = File.createTempFile(cacheName, null);
                // System.out.println(String.format("Creating temporary file for event cache: %s", tempBufferFile));
                objectOutputStreamRef.set(new ObjectOutputStream(new FileOutputStream(tempBufferFile)));
                eventCount.set(0);
            }

            if (useCurrentThread) {
                publishEventsFromFile(fileToPublishRef, eventCountInPublishFile);
            } else {
                final LoggingEventCache<T> me = this;
                // Fire off a thread to actually publish to the external stores. Publishing asynchronously will allow
                // the main program to get back to business instead of blocking for the network and file IO.
                executorService.submit(() -> {
                    Thread.currentThread().setName(PUBLISH_THREAD_NAME);
                    me.publishEventsFromFile(fileToPublishRef, eventCountInPublishFile);
                });
            }

        } catch (Throwable t) {
            System.err.println(String.format("Error while publishing cache: %s", t.getMessage()));
            t.printStackTrace();
            success = false;
        }
        return CompletableFuture.completedFuture(success);
    }
}

