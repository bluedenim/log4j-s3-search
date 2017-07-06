package com.van.logging;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.apache.log4j.Level.INFO;


/**
 * The tests here are more than unit tests and span across multiple classes and collaborators
 * in order to ensure that no integration bugs slip by.
 */
@RunWith(PowerMockRunner.class)
public class LoggingEventCacheTest {

    final int EVENT_COUNT = 25;
    final int BATCH_SIZE = 10;

    final int BATCH_PERIOD_SECS = 3;

    final Logger logger = Logger.getLogger("blah");

    /**
     * Make sure batch flushes do not lose any entries.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testBatchFlushing() {
        final List<LoggingEvent> events = new LinkedList<LoggingEvent>();

        IBufferPublisher<LoggingEvent> publisher = new IBufferPublisher<LoggingEvent>() {
            @Override
            public PublishContext startPublish(String cacheName) {
                return new PublishContext("blah",
                    "blah", new String[] {});
            }

            @Override
            public void publish(PublishContext context, int sequence, final LoggingEvent event) {
                events.add(event);
            }

            @Override
            public void endPublish(PublishContext context) {
            }
        };
        LoggingEventCache cache = new LoggingEventCache(
            "blah", new CapacityBasedBufferMonitor(BATCH_SIZE), publisher);
        for (int i = 0; i < EVENT_COUNT; i++) {
            cache.add(new LoggingEvent("org.van.Blah", logger, INFO,
                String.format("Event %d", i),
                null));
        }
        cache.flushAndPublish();
        // The events list should contain eventCount entries if we published without skipping
        Assert.assertEquals("All events published", EVENT_COUNT, events.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testTimeFlushing() {
        final List<LoggingEvent> events = new LinkedList<LoggingEvent>();
        IBufferPublisher<LoggingEvent> publisher = new IBufferPublisher<LoggingEvent>() {
            @Override
            public PublishContext startPublish(String cacheName) {
                return new PublishContext("blah",
                    "blah", new String[] {});
            }

            @Override
            public void publish(PublishContext context, int sequence, final LoggingEvent event) {
                events.add(event);
            }

            @Override
            public void endPublish(PublishContext context) {
            }
        };
        LoggingEventCache cache = new LoggingEventCache(
            "blah", new TimePeriodBasedBufferMonitor(BATCH_PERIOD_SECS, TimeUnit.SECONDS), publisher);
        long start = System.currentTimeMillis();
        for (int i = 0; i < EVENT_COUNT; i++) {
            cache.add(new LoggingEvent("org.van.Blah", logger, INFO,
                String.format("Event %d", i),
                null));
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        long now = System.currentTimeMillis();
        while (now - start < 10000) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            now = System.currentTimeMillis();
        }
        Assert.assertEquals("All events published", EVENT_COUNT, events.size());
    }
}
