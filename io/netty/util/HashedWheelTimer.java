
package io.netty.util;

import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import io.netty.monitor.EventRateMonitor;
import io.netty.monitor.MonitorName;
import io.netty.monitor.MonitorRegistry;
import io.netty.monitor.ValueDistributionMonitor;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.internal.DetectionUtil;
import io.netty.util.internal.SharedResourceMisuseDetector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class HashedWheelTimer
implements Timer {
    static final InternalLogger logger = InternalLoggerFactory.getInstance(HashedWheelTimer.class);
    private static final SharedResourceMisuseDetector misuseDetector = new SharedResourceMisuseDetector(HashedWheelTimer.class);
    private static final MonitorName TIMEOUT_EXPIRATION_TIME_DEVIATION_MN = new MonitorName(HashedWheelTimer.class, "timeout-expiration-time-deviation");
    private static final MonitorName TIMEOUTS_PER_SECOND_MN = new MonitorName(HashedWheelTimer.class, "timeouts-per-second");
    private final Worker worker;
    final Thread workerThread;
    final AtomicInteger workerState;
    private final long roundDuration;
    final long tickDuration;
    final Set<HashedWheelTimeout>[] wheel;
    final int mask;
    final ReadWriteLock lock;
    volatile int wheelCursor;
    final ValueDistributionMonitor timeoutExpirationTimeDeviation;
    final EventRateMonitor timeoutsPerSecond;

    public HashedWheelTimer() {
        this(Executors.defaultThreadFactory());
    }

    public HashedWheelTimer(long tickDuration, TimeUnit unit) {
        this(Executors.defaultThreadFactory(), tickDuration, unit);
    }

    public HashedWheelTimer(long tickDuration, TimeUnit unit, int ticksPerWheel) {
        this(Executors.defaultThreadFactory(), tickDuration, unit, ticksPerWheel);
    }

    public HashedWheelTimer(ThreadFactory threadFactory) {
        this(threadFactory, 100, TimeUnit.MILLISECONDS);
    }

    public HashedWheelTimer(ThreadFactory threadFactory, long tickDuration, TimeUnit unit) {
        this(threadFactory, tickDuration, unit, 512);
    }

    public HashedWheelTimer(ThreadFactory threadFactory, long tickDuration, TimeUnit unit, int ticksPerWheel) {
        this(threadFactory, tickDuration, unit, ticksPerWheel, MonitorRegistry.NOOP);
    }

    public HashedWheelTimer(ThreadFactory threadFactory, long tickDuration, TimeUnit unit, int ticksPerWheel, MonitorRegistry monitorRegistry) {
        this(threadFactory, tickDuration, unit, ticksPerWheel, monitorRegistry, TIMEOUT_EXPIRATION_TIME_DEVIATION_MN, TIMEOUTS_PER_SECOND_MN);
    }

    public HashedWheelTimer(ThreadFactory threadFactory, long tickDuration, TimeUnit unit, int ticksPerWheel, MonitorRegistry monitorRegistry, MonitorName timeoutExpirationTimeDeviationMonitorName, MonitorName timeoutsPerSecondMonitorName) {
        this.worker = new Worker();
        this.workerState = new AtomicInteger();
        this.lock = new ReentrantReadWriteLock();
        if (threadFactory == null) {
            throw new NullPointerException("threadFactory");
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (monitorRegistry == null) {
            throw new NullPointerException("monitorRegistry");
        }
        if (timeoutExpirationTimeDeviationMonitorName == null) {
            throw new NullPointerException("timeoutExpirationTimeDeviationMonitorName");
        }
        if (timeoutsPerSecondMonitorName == null) {
            throw new NullPointerException("timeoutsPerSecondMonitorName");
        }
        if (tickDuration <= 0) {
            throw new IllegalArgumentException("tickDuration must be greater than 0: " + tickDuration);
        }
        if (ticksPerWheel <= 0) {
            throw new IllegalArgumentException("ticksPerWheel must be greater than 0: " + ticksPerWheel);
        }
        this.wheel = HashedWheelTimer.createWheel(ticksPerWheel);
        this.mask = this.wheel.length - 1;
        this.tickDuration = tickDuration = unit.toMillis(tickDuration);
        if (tickDuration == Long.MAX_VALUE || tickDuration >= Long.MAX_VALUE / (long)this.wheel.length) {
            throw new IllegalArgumentException("tickDuration is too long: " + tickDuration + ' ' + (Object)((Object)unit));
        }
        this.roundDuration = tickDuration * (long)this.wheel.length;
        this.workerThread = threadFactory.newThread(this.worker);
        this.timeoutExpirationTimeDeviation = monitorRegistry.newValueDistributionMonitor(timeoutExpirationTimeDeviationMonitorName);
        this.timeoutsPerSecond = monitorRegistry.newEventRateMonitor(timeoutsPerSecondMonitorName, TimeUnit.SECONDS);
        misuseDetector.increase();
    }

    private static Set<HashedWheelTimeout>[] createWheel(int ticksPerWheel) {
        if (ticksPerWheel <= 0) {
            throw new IllegalArgumentException("ticksPerWheel must be greater than 0: " + ticksPerWheel);
        }
        if (ticksPerWheel > 1073741824) {
            throw new IllegalArgumentException("ticksPerWheel may not be greater than 2^30: " + ticksPerWheel);
        }
        ticksPerWheel = HashedWheelTimer.normalizeTicksPerWheel(ticksPerWheel);
        Set[] wheel = new Set[ticksPerWheel];
        for (int i = 0; i < wheel.length; ++i) {
            wheel[i] = Collections.newSetFromMap(new ConcurrentHashMap(16, 0.95f, 4));
        }
        return wheel;
    }

    private static int normalizeTicksPerWheel(int ticksPerWheel) {
        int normalizedTicksPerWheel;
        for (normalizedTicksPerWheel = 1; normalizedTicksPerWheel < ticksPerWheel; normalizedTicksPerWheel <<= 1) {
        }
        return normalizedTicksPerWheel;
    }

    public void start() {
        switch (this.workerState.get()) {
            case 0: {
                if (!this.workerState.compareAndSet(0, 1)) break;
                this.workerThread.start();
                break;
            }
            case 1: {
                break;
            }
            case 2: {
                throw new IllegalStateException("cannot be started once stopped");
            }
            default: {
                throw new Error();
            }
        }
    }

    @Override
    public Set<Timeout> stop() {
        if (Thread.currentThread() == this.workerThread) {
            throw new IllegalStateException(HashedWheelTimer.class.getSimpleName() + ".stop() cannot be called from " + TimerTask.class.getSimpleName());
        }
        if (!this.workerState.compareAndSet(1, 2)) {
            this.workerState.set(2);
            return Collections.emptySet();
        }
        boolean interrupted = false;
        while (this.workerThread.isAlive()) {
            this.workerThread.interrupt();
            try {
                this.workerThread.join(100);
            }
            catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        misuseDetector.decrease();
        HashSet<HashedWheelTimeout> unprocessedTimeouts = new HashSet<HashedWheelTimeout>();
        for (Set<HashedWheelTimeout> bucket : this.wheel) {
            unprocessedTimeouts.addAll(bucket);
            bucket.clear();
        }
        return Collections.unmodifiableSet(unprocessedTimeouts);
    }

    @Override
    public Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
        long currentTime = System.currentTimeMillis();
        if (task == null) {
            throw new NullPointerException("task");
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        this.start();
        delay = unit.toMillis(delay);
        HashedWheelTimeout timeout = new HashedWheelTimeout(task, currentTime + delay);
        this.scheduleTimeout(timeout, delay);
        return timeout;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void scheduleTimeout(HashedWheelTimeout timeout, long delay) {
        if (delay < this.tickDuration) {
            delay = this.tickDuration;
        }
        long lastRoundDelay = delay % this.roundDuration;
        long lastTickDelay = delay % this.tickDuration;
        long relativeIndex = lastRoundDelay / this.tickDuration + (long)(lastTickDelay != 0) ? 1 : 0;
        long remainingRounds = delay / this.roundDuration - (long)(delay % this.roundDuration == 0) ? 1 : 0;
        this.lock.readLock().lock();
        try {
            int stopIndex;
            timeout.stopIndex = stopIndex = (int)((long)this.wheelCursor + relativeIndex & (long)this.mask);
            timeout.remainingRounds = remainingRounds;
            this.wheel[stopIndex].add(timeout);
        }
        finally {
            this.lock.readLock().unlock();
        }
    }

    private final class HashedWheelTimeout
    implements Timeout {
        private static final int ST_INIT = 0;
        private static final int ST_CANCELLED = 1;
        private static final int ST_EXPIRED = 2;
        private final TimerTask task;
        final long deadline;
        volatile int stopIndex;
        volatile long remainingRounds;
        private final AtomicInteger state;

        HashedWheelTimeout(TimerTask task, long deadline) {
            this.state = new AtomicInteger(0);
            this.task = task;
            this.deadline = deadline;
        }

        @Override
        public Timer getTimer() {
            return HashedWheelTimer.this;
        }

        @Override
        public TimerTask getTask() {
            return this.task;
        }

        @Override
        public boolean cancel() {
            if (!this.state.compareAndSet(0, 1)) {
                return false;
            }
            HashedWheelTimer.this.wheel[this.stopIndex].remove(this);
            return true;
        }

        @Override
        public boolean isCancelled() {
            return this.state.get() == 1;
        }

        @Override
        public boolean isExpired() {
            return this.state.get() != 0;
        }

        public void expire() {
            block3 : {
                if (!this.state.compareAndSet(0, 2)) {
                    return;
                }
                try {
                    HashedWheelTimer.this.timeoutsPerSecond.event();
                    HashedWheelTimer.this.timeoutExpirationTimeDeviation.update(System.currentTimeMillis() - this.deadline);
                    this.task.run(this);
                }
                catch (Throwable t) {
                    if (!HashedWheelTimer.logger.isWarnEnabled()) break block3;
                    HashedWheelTimer.logger.warn("An exception was thrown by " + TimerTask.class.getSimpleName() + '.', t);
                }
            }
        }

        public String toString() {
            long currentTime = System.currentTimeMillis();
            long remaining = this.deadline - currentTime;
            StringBuilder buf = new StringBuilder(192);
            buf.append(this.getClass().getSimpleName());
            buf.append('(');
            buf.append("deadline: ");
            if (remaining > 0) {
                buf.append(remaining);
                buf.append(" ms later, ");
            } else if (remaining < 0) {
                buf.append(- remaining);
                buf.append(" ms ago, ");
            } else {
                buf.append("now, ");
            }
            if (this.isCancelled()) {
                buf.append(", cancelled");
            }
            return buf.append(')').toString();
        }
    }

    private final class Worker
    implements Runnable {
        private long startTime;
        private long tick;

        Worker() {
        }

        @Override
        public void run() {
            ArrayList<HashedWheelTimeout> expiredTimeouts = new ArrayList<HashedWheelTimeout>();
            this.startTime = System.currentTimeMillis();
            this.tick = 1;
            while (HashedWheelTimer.this.workerState.get() == 1) {
                long deadline = this.waitForNextTick();
                if (deadline <= 0) continue;
                this.fetchExpiredTimeouts(expiredTimeouts, deadline);
                this.notifyExpiredTimeouts(expiredTimeouts);
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        private void fetchExpiredTimeouts(List<HashedWheelTimeout> expiredTimeouts, long deadline) {
            HashedWheelTimer.this.lock.writeLock().lock();
            try {
                int newWheelCursor = HashedWheelTimer.this.wheelCursor = HashedWheelTimer.this.wheelCursor + 1 & HashedWheelTimer.this.mask;
                this.fetchExpiredTimeouts(expiredTimeouts, HashedWheelTimer.this.wheel[newWheelCursor].iterator(), deadline);
            }
            finally {
                HashedWheelTimer.this.lock.writeLock().unlock();
            }
        }

        private void fetchExpiredTimeouts(List<HashedWheelTimeout> expiredTimeouts, Iterator<HashedWheelTimeout> i, long deadline) {
            ArrayList<HashedWheelTimeout> slipped = null;
            while (i.hasNext()) {
                HashedWheelTimeout timeout = i.next();
                if (timeout.remainingRounds <= 0) {
                    i.remove();
                    if (timeout.deadline <= deadline) {
                        expiredTimeouts.add(timeout);
                        continue;
                    }
                    if (slipped == null) {
                        slipped = new ArrayList<HashedWheelTimeout>();
                    }
                    slipped.add(timeout);
                    continue;
                }
                --timeout.remainingRounds;
            }
            if (slipped != null) {
                for (HashedWheelTimeout timeout : slipped) {
                    HashedWheelTimer.this.scheduleTimeout(timeout, timeout.deadline - deadline);
                }
            }
        }

        private void notifyExpiredTimeouts(List<HashedWheelTimeout> expiredTimeouts) {
            for (int i = expiredTimeouts.size() - 1; i >= 0; --i) {
                expiredTimeouts.get(i).expire();
            }
            expiredTimeouts.clear();
        }

        /*
         * Enabled aggressive block sorting
         * Enabled unnecessary exception pruning
         * Enabled aggressive exception aggregation
         * Lifted jumps to return sites
         */
        private long waitForNextTick() {
            long deadline = this.startTime + HashedWheelTimer.this.tickDuration * this.tick;
            do {
                long currentTime = System.currentTimeMillis();
                long sleepTime = HashedWheelTimer.this.tickDuration * this.tick - (currentTime - this.startTime);
                if (DetectionUtil.isWindows()) {
                    sleepTime = sleepTime / 10 * 10;
                }
                if (sleepTime <= 0) {
                    ++this.tick;
                    return deadline;
                }
                try {
                    Thread.sleep(sleepTime);
                    continue;
                }
                catch (InterruptedException e) {
                    if (HashedWheelTimer.this.workerState.get() != 1) return -1;
                    continue;
                }
                break;
            } while (true);
        }
    }

}

