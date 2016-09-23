
package io.netty.channel;

import io.netty.channel.ChannelTaskScheduler;
import io.netty.channel.EventExecutor;
import io.netty.channel.EventExecutorGroup;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public abstract class SingleThreadEventExecutor
extends AbstractExecutorService
implements EventExecutor {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(SingleThreadEventExecutor.class);
    private static final long SHUTDOWN_DELAY_NANOS = TimeUnit.SECONDS.toNanos(2);
    static final ThreadLocal<SingleThreadEventExecutor> CURRENT_EVENT_LOOP = new ThreadLocal<T>();
    private static final int ST_NOT_STARTED = 1;
    private static final int ST_STARTED = 2;
    private static final int ST_SHUTDOWN = 3;
    private static final int ST_TERMINATED = 4;
    private static final Runnable WAKEUP_TASK = new Runnable(){

        @Override
        public void run() {
        }
    };
    private final EventExecutorGroup parent;
    private final Queue<Runnable> taskQueue;
    private final Thread thread;
    private final Object stateLock = new Object();
    private final Semaphore threadLock = new Semaphore(0);
    private final ChannelTaskScheduler scheduler;
    private final Set<Runnable> shutdownHooks = new LinkedHashSet<Runnable>();
    private volatile int state = 1;
    private long lastAccessTimeNanos;

    public static SingleThreadEventExecutor currentEventLoop() {
        return CURRENT_EVENT_LOOP.get();
    }

    protected SingleThreadEventExecutor(EventExecutorGroup parent, ThreadFactory threadFactory, ChannelTaskScheduler scheduler) {
        if (threadFactory == null) {
            throw new NullPointerException("threadFactory");
        }
        if (scheduler == null) {
            throw new NullPointerException("scheduler");
        }
        this.parent = parent;
        this.scheduler = scheduler;
        this.thread = threadFactory.newThread(new Runnable(){

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             * Enabled aggressive block sorting
             * Enabled unnecessary exception pruning
             * Enabled aggressive exception aggregation
             */
            @Override
            public void run() {
                SingleThreadEventExecutor.CURRENT_EVENT_LOOP.set(SingleThreadEventExecutor.this);
                boolean success = false;
                try {
                    SingleThreadEventExecutor.this.run();
                    success = true;
                }
                catch (Throwable t) {
                    logger.warn("Unexpected exception from an event executor: ", t);
                    SingleThreadEventExecutor.this.shutdown();
                }
                finally {
                    if (success && SingleThreadEventExecutor.this.lastAccessTimeNanos == 0) {
                        logger.error("Buggy " + EventExecutor.class.getSimpleName() + " implementation; " + SingleThreadEventExecutor.class.getSimpleName() + ".confirmShutdown() must be called " + "before run() implementation terminates.");
                    }
                    while (!SingleThreadEventExecutor.this.confirmShutdown()) {
                    }
                    Object object = SingleThreadEventExecutor.this.stateLock;
                    synchronized (object) {
                        SingleThreadEventExecutor.this.state = 4;
                    }
                }
            }
        });
        this.taskQueue = this.newTaskQueue();
    }

    protected Queue<Runnable> newTaskQueue() {
        return new LinkedBlockingQueue<Runnable>();
    }

    @Override
    public EventExecutorGroup parent() {
        return this.parent;
    }

    @Override
    public EventExecutor next() {
        return this;
    }

    protected void interruptThread() {
        this.thread.interrupt();
    }

    protected Runnable pollTask() {
        assert (this.inEventLoop());
        return this.taskQueue.poll();
    }

    protected Runnable takeTask() throws InterruptedException {
        assert (this.inEventLoop());
        if (this.taskQueue instanceof BlockingQueue) {
            return (Runnable)((BlockingQueue)this.taskQueue).take();
        }
        throw new UnsupportedOperationException();
    }

    protected Runnable peekTask() {
        assert (this.inEventLoop());
        return this.taskQueue.peek();
    }

    protected boolean hasTasks() {
        assert (this.inEventLoop());
        return !this.taskQueue.isEmpty();
    }

    protected void addTask(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task");
        }
        if (this.isTerminated()) {
            SingleThreadEventExecutor.reject();
        }
        this.taskQueue.add(task);
    }

    protected boolean removeTask(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task");
        }
        return this.taskQueue.remove(task);
    }

    protected boolean runAllTasks() {
        Runnable task;
        boolean ran = false;
        while ((task = this.pollTask()) != null) {
            if (task == WAKEUP_TASK) continue;
            try {
                task.run();
                ran = true;
            }
            catch (Throwable t) {
                logger.warn("A task raised an exception.", t);
            }
        }
        return ran;
    }

    protected abstract void run();

    protected void cleanup() {
    }

    protected void wakeup(boolean inEventLoop) {
        if (!inEventLoop || this.state == 3) {
            this.addTask(WAKEUP_TASK);
        }
    }

    @Override
    public boolean inEventLoop() {
        return this.inEventLoop(Thread.currentThread());
    }

    @Override
    public boolean inEventLoop(Thread thread) {
        return thread == this.thread;
    }

    public void addShutdownHook(final Runnable task) {
        if (this.inEventLoop()) {
            this.shutdownHooks.add(task);
        } else {
            this.execute(new Runnable(){

                @Override
                public void run() {
                    SingleThreadEventExecutor.this.shutdownHooks.add(task);
                }
            });
        }
    }

    public void removeShutdownHook(final Runnable task) {
        if (this.inEventLoop()) {
            this.shutdownHooks.remove(task);
        } else {
            this.execute(new Runnable(){

                @Override
                public void run() {
                    SingleThreadEventExecutor.this.shutdownHooks.remove(task);
                }
            });
        }
    }

    private boolean runShutdownHooks() {
        boolean ran = false;
        while (!this.shutdownHooks.isEmpty()) {
            ArrayList<Runnable> copy = new ArrayList<Runnable>(this.shutdownHooks);
            this.shutdownHooks.clear();
            for (Runnable task : copy) {
                try {
                    task.run();
                    ran = true;
                }
                catch (Throwable t) {
                    logger.warn("Shutdown hook raised an exception.", t);
                }
            }
        }
        return ran;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void shutdown() {
        if (this.isShutdown()) {
            return;
        }
        boolean inEventLoop = this.inEventLoop();
        boolean wakeup = true;
        if (inEventLoop) {
            Object object = this.stateLock;
            synchronized (object) {
                assert (this.state == 2);
                this.state = 3;
            }
        }
        Object object = this.stateLock;
        synchronized (object) {
            switch (this.state) {
                case 1: {
                    this.state = 3;
                    this.thread.start();
                    break;
                }
                case 2: {
                    this.state = 3;
                    break;
                }
                default: {
                    wakeup = false;
                }
            }
        }
        if (wakeup) {
            this.wakeup(inEventLoop);
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        this.shutdown();
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown() {
        return this.state >= 3;
    }

    @Override
    public boolean isTerminated() {
        return this.state == 4;
    }

    protected boolean confirmShutdown() {
        if (!this.isShutdown()) {
            throw new IllegalStateException("must be invoked after shutdown()");
        }
        if (!this.inEventLoop()) {
            throw new IllegalStateException("must be invoked from an event loop");
        }
        if (this.runAllTasks() || this.runShutdownHooks()) {
            this.lastAccessTimeNanos = 0;
            this.wakeup(true);
            return false;
        }
        if (this.lastAccessTimeNanos == 0 || System.nanoTime() - this.lastAccessTimeNanos < SHUTDOWN_DELAY_NANOS) {
            if (this.lastAccessTimeNanos == 0) {
                this.lastAccessTimeNanos = System.nanoTime();
            }
            this.wakeup(true);
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                // empty catch block
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (this.inEventLoop()) {
            throw new IllegalStateException("cannot await termination of the current thread");
        }
        if (this.threadLock.tryAcquire(timeout, unit)) {
            this.threadLock.release();
        }
        return this.isTerminated();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task");
        }
        if (this.inEventLoop()) {
            this.addTask(task);
            this.wakeup(true);
        } else {
            Object object = this.stateLock;
            synchronized (object) {
                if (this.state == 1) {
                    this.state = 2;
                    this.thread.start();
                }
            }
            this.addTask(task);
            if (this.isTerminated() && this.removeTask(task)) {
                SingleThreadEventExecutor.reject();
            }
            this.wakeup(false);
        }
    }

    private static void reject() {
        throw new RejectedExecutionException("event executor terminated");
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return this.scheduler.schedule((EventExecutor)this, command, delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return this.scheduler.schedule((EventExecutor)this, callable, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return this.scheduler.scheduleAtFixedRate(this, command, initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return this.scheduler.scheduleWithFixedDelay(this, command, initialDelay, delay, unit);
    }

    static /* synthetic */ Semaphore access$400(SingleThreadEventExecutor x0) {
        return x0.threadLock;
    }

    static /* synthetic */ Queue access$500(SingleThreadEventExecutor x0) {
        return x0.taskQueue;
    }

}

