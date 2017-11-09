package com.van.logging;

import java.io.Serializable;
import java.util.Objects;

/**
 * An event to be published. This class is an agnostic version of Log4j's LoggingEvent.
 */
public class Event implements Serializable {

    static final long serialVersionUID = 1000L;

    private final String source;
    private final String type;
    private final String threadName;

    private final String message;

    public Event(String source, String type, String message) {
        this(source, type, message, Thread.currentThread().getName());
    }

    public Event(String source, String type, String message, String threadName) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(type);
        Objects.requireNonNull(message);
        Objects.requireNonNull(threadName);
        this.source = source;
        this.type = type;
        this.threadName = threadName;
        this.message = message;
    }

    public String getSource() {
        return source;
    }

    public String getType() {
        return type;
    }

    public String getThreadName() {
        return threadName;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return String.format("%s %s:%s:%s", threadName, source, type, message);
    }
}
