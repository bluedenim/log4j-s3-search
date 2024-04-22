package com.van.logging.log4j2;

import com.van.logging.Event;
import com.van.logging.LoggingEventCache;
import com.van.logging.VansLogger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;

import java.io.Serializable;
import java.util.Objects;

/**
 * The composite appender used to route log messages to various outlets. The name will have "CVL" prepended for
 * name spacing when configuring.
 */
@Plugin(name = "Log4j2Appender", category = "Core", elementType = "appender")
public class Log4j2Appender extends AbstractAppender {

    private final LoggingEventCache eventCache;
    private final Thread shutdownHook;
    private boolean verbose = false;

    @PluginBuilderFactory
    public static org.apache.logging.log4j.core.util.Builder<Log4j2Appender> newBuilder() {
        return new Log4j2AppenderBuilder();
    }

    Log4j2Appender(
        String name,
        Filter filter,
        Layout<? extends Serializable> layout,
        boolean ignoreExceptions,
        LoggingEventCache eventCache
    ) {
        super(name, filter, layout, ignoreExceptions);
        Objects.requireNonNull(eventCache);
        this.eventCache = eventCache;
        this.shutdownHook = new Thread(() -> {
            try {
                close();
                LoggingEventCache.shutDown();
            } catch (InterruptedException e) {
                if (this.verbose) {
                    VansLogger.logger.error("InterruptedException during LoggingEventCache.shutDown", e);
                }
            }
        });
        Runtime.getRuntime().addShutdownHook(this.shutdownHook);
    }

    public Log4j2Appender setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    @Override
    public void append(LogEvent logEvent) {
        try {
            eventCache.add(mapToEvent(logEvent));
        } catch (Exception ex) {
            VansLogger.logger.error("Cannot append to event cache", ex);
        }
        if (this.verbose) {
            VansLogger.logger.info(
                String.format("Log4j2Appender says: %s", logEvent.getMessage().getFormattedMessage())
            );
        }
    }

    @Override
    public void stop() {
        close();
        super.stop();
    }

    Event mapToEvent(LogEvent event) {
        String message;
        if (null != getLayout()) {
            message = getLayout().toSerializable(event).toString();
        } else {
            message = event.getMessage().toString();
        }
        Event mapped = new Event(
            event.getLoggerName(),
            event.getLevel().toString(),
            message
        );
        return mapped;
    }

    /**
     * Closes resources used by this appender instance.
     */
    private void close() {
        if (this.verbose) {
            VansLogger.logger.info("Publishing staging log on close...");
        }

        // Deregister shutdown hook to avoid unbounded growth of registered hooks
        if (!isShuttingDown()) {
            try {
                Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
            } catch (final Exception e) {
                // Swallow the exception cause
                VansLogger.logger.error("Error while removing shutdown hook", e);
            }
        }

        // Flush and stop the cache associated with this appender
        eventCache.flushAndPublish(true);
        eventCache.stop();
    }

    /**
     * Tests if the application is currently in the process of shutting down to avoid an
     * {@link IllegalStateException} when attempting to deregister the real shutdown hook(s).
     * @return {@code true} if the JVM  is in the processing of stopping or {@code false} otherwise.
     */
    private boolean isShuttingDown() {
        try {
            final Thread dummyHook = new Thread();
            Runtime.getRuntime().addShutdownHook(dummyHook);
            Runtime.getRuntime().removeShutdownHook(dummyHook);
        } catch (final Exception e) {
            return true;
        }

        return false;
    }
}
