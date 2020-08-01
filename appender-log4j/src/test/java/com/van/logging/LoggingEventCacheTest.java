package com.van.logging;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.apache.log4j.Level.INFO;


class PublisherWithPublishCount implements IBufferPublisher<LoggingEvent> {
    final List<LoggingEvent> events = new LinkedList<>();

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

    public int getPublishedEventCount() {
        return events.size();
    }
}


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
    PublisherWithPublishCount publisher;

    @Before
    public void setUp() {
        publisher = new PublisherWithPublishCount();
    }

    /**
     * Make sure batch flushes do not lose any entries.
     */
    @Test
    public void testBatchFlushing() {
        try {
            LoggingEventCache<LoggingEvent> cache = new LoggingEventCache<>(
                "blah", new CapacityBasedBufferMonitor<>(BATCH_SIZE), publisher);
            for (int i = 0; i < EVENT_COUNT; i++) {
                cache.add(new LoggingEvent("org.van.Blah", logger, INFO,
                    String.format("Event %d", i),
                    null));
            }
            cache.flushAndPublish();
            Thread.sleep(1000); // Give the publishing thread some time to finish
            // The events list should contain eventCount entries if we published without skipping
            Assert.assertEquals("All events published", EVENT_COUNT, publisher.getPublishedEventCount());
        } catch (Exception ex) {
            Assert.fail(String.format("Unexpected exception: %s", ex));
        }
    }

    @Test
    public void testTimeFlushing() {
        try {
            LoggingEventCache<LoggingEvent> cache = new LoggingEventCache<>(
                "blah", new TimePeriodBasedBufferMonitor<>(BATCH_PERIOD_SECS, TimeUnit.SECONDS), publisher);
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
            Assert.assertEquals("All events published", EVENT_COUNT, publisher.getPublishedEventCount());
        } catch (Exception ex) {
            Assert.fail(String.format("Unexpected exception: %s", ex));
        }
    }

    @Test
    public void testPublishInCurrentThread() {
        try {
            LoggingEventCache<LoggingEvent> cache = new LoggingEventCache<>(
                "blah", new CapacityBasedBufferMonitor<>(BATCH_SIZE), publisher);

            int eventsToPublish = BATCH_SIZE / 2;
            for (int i = 0; i < eventsToPublish; i++) {
                cache.add(new LoggingEvent("org.van.Blah", logger, INFO,
                    String.format("Event %d", i),
                    null));
            }
            cache.flushAndPublish(true);
            Assert.assertEquals("Correct number of events published",
                eventsToPublish, publisher.getPublishedEventCount());

        } catch (Exception ex) {
            Assert.fail(String.format("Unexpected exception: %s", ex));
        }
    }
}
