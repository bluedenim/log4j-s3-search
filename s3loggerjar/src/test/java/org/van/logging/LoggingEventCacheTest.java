package org.van.logging;

import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.LinkedList;
import java.util.List;

import static org.apache.log4j.Level.INFO;

@RunWith(PowerMockRunner.class)
public class LoggingEventCacheTest {

    /**
     * Make sure batch flushes do not lose any entries.
     */
    @Test
    public void testBatchFlushing() {
        final int EVENT_COUNT = 25;
        final int BATCH_SIZE = 10;
        final List<LoggingEvent> events = new LinkedList<LoggingEvent>();
        final Logger logger = Logger.getLogger("blah");
        LoggingEventCache.ICachePublisher publisher = new LoggingEventCache.ICachePublisher() {
            @Override
            public PublishContext startPublish(String cacheName) {
                return new PublishContext("blah",
                    "blah", new String[] {}, new SimpleLayout());
            }

            @Override
            public void publish(PublishContext context, int sequence, LoggingEvent event) {
                events.add(event);
            }

            @Override
            public void endPublish(PublishContext context) {
            }
        };
        LoggingEventCache cache = new LoggingEventCache("blah", BATCH_SIZE, publisher);
        for (int i = 0; i < EVENT_COUNT; i++) {
            cache.add(new LoggingEvent("org.van.Blah", logger, INFO,
                String.format("Event %d", i),
                null));
        }
        cache.flushAndPublishQueue(false);
        // The events list should contain eventCount entries if we published without skipping
        Assert.assertEquals("Events list should contain all entries", EVENT_COUNT, events.size());
    }
}
