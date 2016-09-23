
package io.netty.channel;

import io.netty.channel.EventExecutor;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class ChannelTaskScheduler {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ChannelTaskScheduler.class);
    private static final long SCHEDULE_PURGE_INTERVAL = TimeUnit.SECONDS.toNanos(1);
    private static final long START_TIME = System.nanoTime();
    private static final AtomicLong nextTaskId = new AtomicLong();
    private final BlockingQueue<ScheduledFutureTask<?>> taskQueue = new DelayQueue();
    private final Thread thread;
    private final Object stateLock = new Object();
    private final Semaphore threadLock = new Semaphore(0);
    private volatile int state;

    private static long nanoTime() {
        return System.nanoTime() - START_TIME;
    }

    private static long deadlineNanos(long delay) {
        return ChannelTaskScheduler.nanoTime() + delay;
    }

    public ChannelTaskScheduler(ThreadFactory threadFactory) {
        if (threadFactory == null) {
            throw new NullPointerException("threadFactory");
        }
        this.thread = threadFactory.newThread(new Runnable(){

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void run() {
                Object task;
                try {
                    do {
                        try {
                            task = (ScheduledFutureTask)ChannelTaskScheduler.this.taskQueue.take();
                            this.runTask(task);
                            continue;
                        }
                        catch (InterruptedException e) {
                            // empty catch block
                        }
                    } while (!ChannelTaskScheduler.this.isShutdown() || ChannelTaskScheduler.this.taskQueue.peek() != null);
                }
                finally {
                    try {
                        try {
                            this.cleanupTasks();
                        }
                        finally {
                            task = ChannelTaskScheduler.this.stateLock;
                            synchronized (task) {
                                ChannelTaskScheduler.this.state = 3;
                            }
                        }
                        this.cleanupTasks();
                    }
                    finally {
                        ChannelTaskScheduler.this.threadLock.release();
                        assert (ChannelTaskScheduler.this.taskQueue.isEmpty());
                    }
                }
            }

            private void runTask(ScheduledFutureTask<?> task) {
                EventExecutor executor = task.executor;
                if (executor == null) {
                    task.run();
                } else if (executor.isShutdown()) {
                    task.cancel(false);
                } else {
                    try {
                        task.executor.execute(task);
                    }
                    catch (RejectedExecutionException e) {
                        task.cancel(false);
                    }
                }
            }

            private void cleanupTasks() {
                boolean ran;
                do {
                    ScheduledFutureTask task;
                    ran = false;
                    ChannelTaskScheduler.this.cancelScheduledTasks();
                    while ((task = (ScheduledFutureTask)ChannelTaskScheduler.this.taskQueue.poll()) != null) {
                        try {
                            this.runTask(task);
                            ran = true;
                        }
                        catch (Throwable t) {
                            logger.warn("A task raised an exception.", t);
                        }
                    }
                } while (ran || !ChannelTaskScheduler.this.taskQueue.isEmpty());
            }
        });
    }

    private boolean inSameThread() {
        return Thread.currentThread() == this.thread;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void shutdown() {
        boolean inSameThread = this.inSameThread();
        boolean wakeup = false;
        if (inSameThread) {
            Object object = this.stateLock;
            synchronized (object) {
                assert (this.state == 1);
                this.state = 2;
                wakeup = true;
            }
        }
        Object object = this.stateLock;
        synchronized (object) {
            switch (this.state) {
                case 0: {
                    this.state = 3;
                    this.threadLock.release();
                    break;
                }
                case 1: {
                    this.state = 2;
                    wakeup = true;
                }
            }
        }
        if (wakeup && !inSameThread && this.isShutdown()) {
            this.thread.interrupt();
        }
    }

    public boolean isShutdown() {
        return this.state >= 2;
    }

    public boolean isTerminated() {
        return this.state == 3;
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (this.inSameThread()) {
            throw new IllegalStateException("cannot await termination of the current thread");
        }
        if (this.threadLock.tryAcquire(timeout, unit)) {
            this.threadLock.release();
        }
        return this.isTerminated();
    }

    public ScheduledFuture<?> schedule(EventExecutor executor, Runnable command, long delay, TimeUnit unit) {
        if (executor == null) {
            throw new NullPointerException("executor");
        }
        if (command == null) {
            throw new NullPointerException("command");
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (delay < 0) {
            throw new IllegalArgumentException(String.format("delay: %d (expected: >= 0)", delay));
        }
        return this.schedule(new ScheduledFutureTask<Object>(this, executor, command, null, ChannelTaskScheduler.deadlineNanos(unit.toNanos(delay))));
    }

    public <V> ScheduledFuture<V> schedule(EventExecutor executor, Callable<V> callable, long delay, TimeUnit unit) {
        if (executor == null) {
            throw new NullPointerException("executor");
        }
        if (callable == null) {
            throw new NullPointerException("callable");
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (delay < 0) {
            throw new IllegalArgumentException(String.format("delay: %d (expected: >= 0)", delay));
        }
        return this.schedule(new ScheduledFutureTask<V>(this, executor, callable, ChannelTaskScheduler.deadlineNanos(unit.toNanos(delay))));
    }

    public ScheduledFuture<?> scheduleAtFixedRate(EventExecutor executor, Runnable command, long initialDelay, long period, TimeUnit unit) {
        if (executor == null) {
            throw new NullPointerException("executor");
        }
        if (command == null) {
            throw new NullPointerException("command");
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (initialDelay < 0) {
            throw new IllegalArgumentException(String.format("initialDelay: %d (expected: >= 0)", initialDelay));
        }
        if (period <= 0) {
            throw new IllegalArgumentException(String.format("period: %d (expected: > 0)", period));
        }
        return this.schedule(new ScheduledFutureTask<Object>(this, executor, command, null, ChannelTaskScheduler.deadlineNanos(unit.toNanos(initialDelay)), unit.toNanos(period)));
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(EventExecutor executor, Runnable command, long initialDelay, long delay, TimeUnit unit) {
        if (executor == null) {
            throw new NullPointerException("executor");
        }
        if (command == null) {
            throw new NullPointerException("command");
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (initialDelay < 0) {
            throw new IllegalArgumentException(String.format("initialDelay: %d (expected: >= 0)", initialDelay));
        }
        if (delay <= 0) {
            throw new IllegalArgumentException(String.format("delay: %d (expected: > 0)", delay));
        }
        return this.schedule(new ScheduledFutureTask<Object>(this, executor, command, null, ChannelTaskScheduler.deadlineNanos(unit.toNanos(initialDelay)), - unit.toNanos(delay)));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private <V> ScheduledFuture<V> schedule(ScheduledFutureTask<V> task) {
        if (this.isShutdown()) {
            ChannelTaskScheduler.reject();
        }
        this.taskQueue.add(task);
        if (this.isShutdown()) {
            task.cancel(false);
        }
        boolean started = false;
        if (!this.inSameThread()) {
            Object object = this.stateLock;
            synchronized (object) {
                if (this.state == 0) {
                    this.state = 1;
                    this.thread.start();
                    started = true;
                }
            }
        }
        if (started) {
            this.schedule(new ScheduledFutureTask<Object>(this, null, new PurgeTask(), null, ChannelTaskScheduler.deadlineNanos(SCHEDULE_PURGE_INTERVAL), - SCHEDULE_PURGE_INTERVAL));
        }
        return task;
    }

    private static void reject() {
        throw new RejectedExecutionException("event executor shut down");
    }

    private void cancelScheduledTasks() {
        if (this.taskQueue.isEmpty()) {
            return;
        }
        for (ScheduledFutureTask task : this.taskQueue.toArray(new ScheduledFutureTask[this.taskQueue.size()])) {
            task.cancel(false);
        }
        this.taskQueue.clear();
    }

    private final class PurgeTask
    implements Runnable {
        private PurgeTask() {
        }

        @Override
        public void run() {
            Iterator i = ChannelTaskScheduler.this.taskQueue.iterator();
            while (i.hasNext()) {
                ScheduledFutureTask task = (ScheduledFutureTask)i.next();
                if (!task.isCancelled()) continue;
                i.remove();
            }
        }
    }

    private class ScheduledFutureTask<V>
    extends FutureTask<V>
    implements ScheduledFuture<V> {
        private final EventExecutor executor;
        private final long id;
        private long deadlineNanos;
        private final long periodNanos;
        final /* synthetic */ ChannelTaskScheduler this$0;

        ScheduledFutureTask(ChannelTaskScheduler channelTaskScheduler, EventExecutor executor, Runnable runnable, V result, long nanoTime) {
            this.this$0 = channelTaskScheduler;
            super(runnable, result);
            this.id = nextTaskId.getAndIncrement();
            this.executor = executor;
            this.deadlineNanos = nanoTime;
            this.periodNanos = 0;
        }

        ScheduledFutureTask(ChannelTaskScheduler channelTaskScheduler, EventExecutor executor, Runnable runnable, V result, long nanoTime, long period) {
            this.this$0 = channelTaskScheduler;
            super(runnable, result);
            this.id = nextTaskId.getAndIncrement();
            if (period == 0) {
                throw new IllegalArgumentException("period: 0 (expected: != 0)");
            }
            this.executor = executor;
            this.deadlineNanos = nanoTime;
            this.periodNanos = period;
        }

        ScheduledFutureTask(ChannelTaskScheduler channelTaskScheduler, EventExecutor executor, Callable<V> callable, long nanoTime) {
            this.this$0 = channelTaskScheduler;
            super(callable);
            this.id = nextTaskId.getAndIncrement();
            this.executor = executor;
            this.deadlineNanos = nanoTime;
            this.periodNanos = 0;
        }

        public long deadlineNanos() {
            return this.deadlineNanos;
        }

        public long delayNanos() {
            return Math.max(0, this.deadlineNanos() - ChannelTaskScheduler.nanoTime());
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(this.delayNanos(), TimeUnit.NANOSECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            if (this == o) {
                return 0;
            }
            ScheduledFutureTask that = (ScheduledFutureTask)o;
            long d = this.deadlineNanos() - that.deadlineNanos();
            if (d < 0) {
                return -1;
            }
            if (d > 0) {
                return 1;
            }
            if (this.id < that.id) {
                return -1;
            }
            if (this.id == that.id) {
                throw new Error();
            }
            return 1;
        }

        @Override
        public void run() {
            if (this.periodNanos == 0) {
                super.run();
            } else {
                boolean reset = this.runAndReset();
                if (reset && !this.this$0.isShutdown()) {
                    long p = this.periodNanos;
                    this.deadlineNanos = p > 0 ? (this.deadlineNanos += p) : ChannelTaskScheduler.nanoTime() - p;
                    this.this$0.schedule(this);
                }
            }
        }
    }

}

