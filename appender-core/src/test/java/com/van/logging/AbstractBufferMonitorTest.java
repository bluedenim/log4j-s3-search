package com.van.logging;

import org.junit.Before;
import java.util.concurrent.Future;

/**
 * Abstract base class for monitor tests. Shared facilities for testing
 * different implementations of BufferMonitors.
 */
public abstract class AbstractBufferMonitorTest {
    protected int publishCount = 0;

    protected IFlushAndPublish publisher = new IFlushAndPublish() {
        @Override
        public Future<Boolean> flushAndPublish(boolean useCurrentThread) {
            System.out.println(String.format("flushAndPublish(useCurrentThread=%s) called.", useCurrentThread));
            publishCount++;
            return null;
        }
    };

    @Before
    public void setUp() {
        System.out.println("resetting publishCount");
        publishCount = 0;
    }


}
