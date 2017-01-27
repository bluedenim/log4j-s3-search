package org.van.logging;

import org.apache.log4j.spi.LoggingEvent;
import org.van.logging.solr.IFlushAndPublish;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of {@link IBufferMonitor} that flushes the cache when a specified time period
 * has transpired since the first event was added.
 * <br/>
 * NOTE that, for this monitor to flush a cache for a time period, the cache must have at least
 * one event added in the period. In fact, the first event added will start a timer that will
 * count down until the time period expires. At which time the cache will be flushed and published.
 * <br/>
 * This means that, if there were ever a relatively "quiet" period when there isn't at least
 * one event added to the cache, then nothing will be flushed and published.
 */
public class TimePeriodBasedBufferMonitor implements IBufferMonitor {

    private final ScheduledExecutorService scheduledExecutorService =
        Executors.newScheduledThreadPool(1);

    private final AtomicReference<ScheduledFuture<Boolean>> scheduledFutureRef =
        new AtomicReference<>();

    static class TimeDurationTuple {
        public final int amount;
        public final TimeUnit timeUnit;

        public TimeDurationTuple(int amount, TimeUnit timeUnit) {
            this.amount = amount;
            this.timeUnit = timeUnit;
        }

        public String toString() {
            return String.format("TimeDurationTuple(%d,%s)",
                amount, timeUnit.name());
        }
    }

    private final TimeDurationTuple timeDurationTuple;


    public TimePeriodBasedBufferMonitor(int minutes) {
        this(minutes, TimeUnit.MINUTES);
    }

    public TimePeriodBasedBufferMonitor(int amount, TimeUnit timeUnit) {
        timeDurationTuple = new TimeDurationTuple(amount, timeUnit);
    }

    @Override
    public void eventAdded(final LoggingEvent event, final IFlushAndPublish publisher) {
        ScheduledFuture<Boolean> scheduledFuture = scheduledFutureRef.get();
        if (null == scheduledFuture) {
            TimeDurationTuple tuple = getSchedulingTimeDuration();
            scheduledFutureRef.set(scheduledExecutorService.schedule(
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        try {
                            publisher.flushAndPublish();
                        } finally {
                            scheduledFutureRef.set(null);
                        }
                        return true;
                    }
                },
                tuple.amount, tuple.timeUnit));
        }
    }

    @Override
    public String toString() {
        return String.format(
            "TimePeriodBasedBufferMonitor(timePeriodMinutes: %s)",
            timeDurationTuple.toString());
    }

    TimeDurationTuple getSchedulingTimeDuration() {
        return timeDurationTuple;
    }
}
