package com.van.logging;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of {@link IBufferMonitor} that flushes the cache when a specified time period
 * has transpired since the first event was added.
 * <br>
 * NOTE that, for this monitor to flush a cache for a time period, the cache must have at least
 * one event added in the period. In fact, the first event added will start a timer that will
 * count down until the time period expires. At which time the cache will be flushed and published.
 * <br>
 * This means that, if there were ever a relatively "quiet" period when there isn't at least
 * one event added to the cache, then nothing will be flushed and published.
 */
public class TimePeriodBasedBufferMonitor<T> implements IBufferMonitor<T> {

    private final ScheduledExecutorService scheduledExecutorService =
        Executors.newScheduledThreadPool(1);

    private final AtomicBoolean monitorStarted = new AtomicBoolean(false);
    private final long periodInSeconds;

    static class TimeDurationTuple {
        public final long amount;
        public final TimeUnit timeUnit;

        public TimeDurationTuple(long amount, TimeUnit timeUnit) {
            this.amount = amount;
            this.timeUnit = timeUnit;
        }

        public String toString() {
            return String.format("TimeDurationTuple(%d,%s)",
                amount, timeUnit.name());
        }
    }

    public TimePeriodBasedBufferMonitor(int minutes) {
        this(minutes, TimeUnit.MINUTES);
    }

    public TimePeriodBasedBufferMonitor(int amount, TimeUnit timeUnit) {
        this.periodInSeconds = timeUnit.toSeconds(amount);
    }

    @Override
    public void eventAdded(final T event, final IFlushAndPublish publisher) {
        if (!monitorStarted.getAndSet(true)) {
            TimeDurationTuple tuple = getSchedulingTimeDuration();
            scheduledExecutorService.scheduleAtFixedRate(
                new Runnable() {
                    @Override
                    public void run() {
                        long started = System.currentTimeMillis();
                        Thread.currentThread().setName("TimePeriodBasedBufferMonitor-publish-trigger");
                        try {
                            publisher.flushAndPublish();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        } finally {
                            long now = System.currentTimeMillis();
                            if (now - started > (periodInSeconds * 9 / 10)) {
                                System.err.println(
                                    "Publish operation is approaching monitor period. Increase " +
                                    "period or risk compromising fixed rate.");
                            }
                        }
                    }
                },
                0L,
                tuple.amount, tuple.timeUnit);
        }
    }

    @Override
    public void shutDown() {
        System.out.println("TimePeriodBasedBufferMonitor: shutting down.");
        this.scheduledExecutorService.shutdownNow();
    }

    @Override
    public String toString() {
        return String.format(
            "TimePeriodBasedBufferMonitor(periodInSeconds: %d)",
            periodInSeconds);
    }

    TimeDurationTuple getSchedulingTimeDuration() {
        return new TimeDurationTuple(periodInSeconds, TimeUnit.SECONDS);
    }
}
