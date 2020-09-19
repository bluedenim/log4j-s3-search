package com.van.logging;

import org.junit.Assert;
import org.junit.Test;

public class LoggingEventCacheTest {
    private static final String CACHE_NAME = "test-cache";

    private static class PublisherStub implements IBufferPublisher<String> {

        @Override
        public PublishContext startPublish(String cacheName) {
            return null;
        }

        @Override
        public void publish(PublishContext context, int sequence, String event) {

        }

        @Override
        public void endPublish(PublishContext context) {

        }
    };

    public static final class TestMonitor implements IBufferMonitor<String> {
        private boolean isShutdown = false;

        @Override
        public void eventAdded(String event, IFlushAndPublish flushAndPublisher) {

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
            LoggingEventCache<String> inst = new LoggingEventCache<>(CACHE_NAME, monitor, new PublisherStub());
            LoggingEventCache.shutDown();

            Assert.assertTrue("Monitor is shut down when LoggingEventCache is shut down", monitor.isShutdown);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Failing on error");
        }
    }
}
