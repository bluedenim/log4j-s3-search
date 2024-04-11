package com.van.logging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
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
public class LoggingEventCache implements IFlushAndPublish {

    public static final String PUBLISH_THREAD_NAME =
        "LoggingEventCache-publish-thread";

    // Do not increase beyond 1 until all implementations of IBufferPublisher are thread safe or a new instance
    // of IBufferPublisher is used for each publish operation.
    private static final int PUBLISHING_THREADS = 1;

    private static final long SHUTDOWN_TIMEOUT_SECS = 10;

    private static final String DEFAULT_TEMP_FILE_PREFIX = "log4j-s3";

    private final String cacheName;

    private File tempBufferFile;
    private final Object bufferLock = new Object();
    private final AtomicReference<ObjectOutputStream> objectOutputStreamRef = new AtomicReference<>();
    private final AtomicInteger eventCount = new AtomicInteger();

    private final IBufferMonitor cacheMonitor;
    private final IBufferPublisher cachePublisher;
    private final boolean verbose;

    private final AtomicReference<ExecutorService> executorServiceRef = new AtomicReference<>(null);

    private static final Queue<LoggingEventCache> instances = new ConcurrentLinkedQueue<>();


    /**
     * Shutdown and cleanup the LoggingEventCache singleton. This is useful to call when a program ends in order to
     * release any threads.
     *
     * @return True if the publishing thread executor service shut down successfully, False if it timed out.
     * @throws InterruptedException if the shutdown process was interrupted.
     */
    public static boolean shutDown() throws InterruptedException {
        boolean success = true;
        LoggingEventCache instance = instances.poll();
        while (null != instance) {
            try {
                instance.stop();
            } catch (Exception ex) {
                VansLogger.logger.error(String.format("LoggingEventCache: error shutting down %s", instance), ex);
            } finally {
                instance = instances.poll();
            }
        }
        return success;
    }

    /**
     * Creates an instance with the provided buffer publishing collaborator.
     * The instance will create a buffer of the capacity specified and will
     * publish a batch when collected events reach that capacity.
     *
     * Currently, only one instance of this class can be created per process. Think of this as a Singleton class.
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
     * @throws IllegalStateException if there is already an instance of the class instantiated
     */
    public LoggingEventCache(String cacheName, IBufferMonitor cacheMonitor,
                             IBufferPublisher cachePublisher) throws Exception {
        this(cacheName, cacheMonitor, cachePublisher, false);
    }

    public LoggingEventCache(String cacheName, IBufferMonitor cacheMonitor,
                             IBufferPublisher cachePublisher,
                             boolean verbose) throws Exception {
        if (null == cacheName) {
            this.cacheName = DEFAULT_TEMP_FILE_PREFIX;
        } else {
            this.cacheName = cacheName;
        }
        this.cacheMonitor = cacheMonitor;
        this.cachePublisher = cachePublisher;
        this.verbose = verbose;

        synchronized(bufferLock) {
            tempBufferFile = File.createTempFile(this.cacheName, null);
            if (VansLogger.logger.isDebugEnabled()) {
                VansLogger.logger.debug(
                    String.format("Creating temporary file for event cache: %s", tempBufferFile)
                );
            }
           this.objectOutputStreamRef.set(
               new ObjectOutputStream(new FileOutputStream(tempBufferFile)));
           this.eventCount.set(0);
        }

        executorServiceRef.set(createExecutorService());
        instances.add(this);
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
    public void add(Event event) throws IOException {
        // Internal logs should not be added to the cache
        // One reason is to reduce noise, but a bigger reason is to avoid potential infinite recursion if
        // there are errors in our code.
        if (!event.getSource().equals(VansLogger.logger.getName())) {
            synchronized (bufferLock) {
                objectOutputStreamRef.get().writeObject(event);
                eventCount.incrementAndGet();
            }
            cacheMonitor.eventAdded(event, this);
        }
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

            if (VansLogger.logger.isDebugEnabled()) {
                VansLogger.logger.debug(String.format("Publishing from file: %s", fileToPublish.getAbsolutePath()));
            }
            try (FileInputStream fis = new FileInputStream(fileToPublish);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                for (int i = 0; i < eventCount.get(); i++) {
                    cachePublisher.publish(context, i, (Event) ois.readObject());
                }
                cachePublisher.endPublish(context);
            } finally {
                try {
                    fileToPublish.delete();
                } catch (Exception ex) {
                }
            }
        } catch (Throwable t) {
            VansLogger.logger.error(
                String.format("Error while publishing cache from publishing thread: %s", t.getMessage()), t
            );
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
                if (VansLogger.logger.isDebugEnabled()) {
                    VansLogger.logger.debug(
                        String.format("Creating temporary file for event cache: %s", tempBufferFile)
                    );
                }
                objectOutputStreamRef.set(new ObjectOutputStream(new FileOutputStream(tempBufferFile)));
                eventCount.set(0);
            }

            if (useCurrentThread) {
                publishEventsFromFile(fileToPublishRef, eventCountInPublishFile);
            } else {
                final LoggingEventCache me = this;
                // Fire off a thread to actually publish to the external stores. Publishing asynchronously will allow
                // the main program to get back to business instead of blocking for the network and file IO.
                ExecutorService executorService = executorServiceRef.get();
                if (null != executorService) {
                    executorService.submit(() -> {
                        Thread.currentThread().setName(PUBLISH_THREAD_NAME);
                        me.publishEventsFromFile(fileToPublishRef, eventCountInPublishFile);
                    });
                } else {
                    throw new RejectedExecutionException("executorServiceRef has null ref.");
                }
            }
        } catch (RejectedExecutionException ex) {
            VansLogger.logger.error("ExecutorService refused submitted task. Was shutDown() called?", ex);
        } catch (Throwable t) {
            VansLogger.logger.error(String.format("Error while publishing cache: %s", t.getMessage()), t);
            success = false;
        }
        return CompletableFuture.completedFuture(success);
    }

    public boolean stop() {
        boolean success = true;
        final ExecutorService executorService = executorServiceRef.getAndSet(null);

        if (executorService != null && !executorService.isShutdown()) {
            try {
                if (verbose) {
                    VansLogger.logger.info(String.format("LoggingEventCache %s: shutting down", executorService));
                }
                executorService.shutdown();
                boolean terminated = executorService.awaitTermination(
                        SHUTDOWN_TIMEOUT_SECS,
                        TimeUnit.SECONDS
                );
                if (verbose) {
                    VansLogger.logger.info(String.format(
                            "LoggingEventCache: Executor service terminated within timeout: %s", terminated
                    ));
                }
                success = success & terminated;
            } catch (Exception ex) {
                VansLogger.logger.error(String.format("LoggingEventCache: error shutting down %s", executorService), ex);
                success = false;
            }
        }

        if (null != cacheMonitor) {
            cacheMonitor.shutDown();
        }

        if (null != instances) {
            // Remove the stopped cache from the queue to avoid a leak
            instances.remove(this);
        }

        return success;
    }
}

