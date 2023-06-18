package com.van.logging;

import org.junit.Assert;
import org.junit.Test;

public class LoggingEventCacheTest {
    private static final String CACHE_NAME = "test-cache";

    private static class PublisherStub implements IBufferPublisher {

        @Override
        public PublishContext startPublish(String cacheName) {
            return null;
        }

        @Override
        public void publish(PublishContext context, int sequence, Event event) {

        }

        @Override
        public void endPublish(PublishContext context) {

        }
    };

    public static final class TestMonitor implements IBufferMonitor {
        private boolean isShutdown = false;

        @Override
        public void eventAdded(Event event, IFlushAndPublish flushAndPublisher) {

        }

        @Override
        public void shutDown() {
            isShutdown = true;
        }
    };


    @Test
    public void testShutdown() {
        final TestMonitor monitor = new TestMonitor();
        try {
            LoggingEventCache inst = new LoggingEventCache(CACHE_NAME, monitor, new PublisherStub(), true);
            LoggingEventCache.shutDown();

            Assert.assertTrue("Monitor is shut down when LoggingEventCache is shut down", monitor.isShutdown);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Failing on error");
        }
    }

    @Test
    public void testShutdownMultipleCaches() {
        final TestMonitor monitor = new TestMonitor();
        final TestMonitor monitor2 = new TestMonitor();
        try {
            LoggingEventCache inst = new LoggingEventCache(CACHE_NAME, monitor, new PublisherStub(), true);
            LoggingEventCache inst2 = new LoggingEventCache(CACHE_NAME, monitor2, new PublisherStub(), true);

            LoggingEventCache.shutDown();

            Assert.assertTrue("Monitor is shut down when LoggingEventCache is shut down", monitor.isShutdown);
            Assert.assertTrue("Monitor2 is shut down when LoggingEventCache is shut down", monitor2.isShutdown);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Failing on error");
        }
    }
}
