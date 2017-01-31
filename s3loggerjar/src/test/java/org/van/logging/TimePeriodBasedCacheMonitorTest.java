package org.van.logging;


import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class TimePeriodBasedCacheMonitorTest extends AbstractBufferMonitorTest {

    /**
     * A test to add an event every second for 10 seconds while having a
     * {@link TimePeriodBasedBufferMonitor} publish every 3 seconds. At the
     * end we expect to have seen at least 3 publishes. Race conditions may
     * or may not allow a 4th publish.
     */
    @Test
    public void testFlush() {
        try {
            TimePeriodBasedBufferMonitor monitor =
                new TimePeriodBasedBufferMonitor(3, TimeUnit.SECONDS);
            long started = System.currentTimeMillis();
            // Add an event every second for 10 seconds
            for (int i = 0; i < 10; i++) {
                monitor.eventAdded(null, publisher);
                try {
                    Thread.currentThread().sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            long now = System.currentTimeMillis();
            // Wait until 12s has elasped
            while (now - started < 12000) {
                try {
                    Thread.currentThread().sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                now = System.currentTimeMillis();
            }
            Assert.assertTrue("multiple flushes called", publishCount >= 3);
        } catch (Exception ex) {
            Assert.fail("Unexpected exception " + ex.getMessage());
        }
    }
}
