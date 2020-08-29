package com.van.logging.log4j2;

import com.van.logging.Event;
import com.van.logging.LoggingEventCache;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * The composite appender used to route log messages to various outlets. The name will have "CVL" prepended for
 * name spacing when configuring.
 */
@Plugin(name = "Log4j2Appender", category = "Core", elementType = "appender")
public class Log4j2Appender extends AbstractAppender {

    private LoggingEventCache<Event> eventCache = null;
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
       LoggingEventCache<Event> eventCache
    ) {
        super(name, filter, layout, ignoreExceptions);
        Objects.requireNonNull(eventCache);
        this.eventCache = eventCache;

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                LOGGER.debug("Publishing staging log on shutdown...");
                eventCache.flushAndPublish(true);
            }
        });
    }

    public void append(LogEvent logEvent) {
        try {
            eventCache.add(mapToEvent(logEvent));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        LOGGER.debug(String.format("Log4j2Appender says: %s", logEvent.getMessage().getFormattedMessage()));
    }



    Event mapToEvent(LogEvent event) {
        String message = null;
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
}
