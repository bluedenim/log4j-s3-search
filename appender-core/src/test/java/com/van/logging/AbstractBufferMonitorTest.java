package com.van.logging;

import org.junit.Before;

/**
 * Abstract base class for monitor tests. Shared facilities for testing
 * different implementations of BufferMonitors.
 */
public abstract class AbstractBufferMonitorTest {
    protected int publishCount = 0;

    protected IFlushAndPublish publisher = new IFlushAndPublish() {
        @Override
        public void flushAndPublish() {
            System.out.println("flushAndPublish() called.");
            publishCount++;
        }
    };

    @Before
    public void setUp() {
        System.out.println("resetting publishCount");
        publishCount = 0;
    }


}
