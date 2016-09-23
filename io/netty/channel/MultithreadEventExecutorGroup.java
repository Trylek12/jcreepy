
package io.netty.channel;

import io.netty.channel.ChannelTaskScheduler;
import io.netty.channel.EventExecutor;
import io.netty.channel.EventExecutorGroup;
import io.netty.channel.EventLoopException;
import io.netty.channel.SingleThreadEventExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class MultithreadEventExecutorGroup
implements EventExecutorGroup {
    private static final int DEFAULT_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
    private static final AtomicInteger poolId = new AtomicInteger();
    final ChannelTaskScheduler scheduler;
    private final EventExecutor[] children;
    private final AtomicInteger childIndex = new AtomicInteger();

    protected /* varargs */ MultithreadEventExecutorGroup(int nThreads, ThreadFactory threadFactory, Object ... args) {
        if (nThreads < 0) {
            throw new IllegalArgumentException(String.format("nThreads: %d (expected: >= 0)", nThreads));
        }
        if (nThreads == 0) {
            nThreads = DEFAULT_POOL_SIZE;
        }
        if (threadFactory == null) {
            threadFactory = new DefaultThreadFactory();
        }
        this.scheduler = new ChannelTaskScheduler(threadFactory);
        this.children = new SingleThreadEventExecutor[nThreads];
        for (int i = 0; i < nThreads; ++i) {
            boolean success = false;
            try {
                this.children[i] = this.newChild(threadFactory, this.scheduler, args);
                success = true;
            }
            catch (Exception e) {
                throw new EventLoopException("failed to create a child event loop", e);
            }
            finally {
                if (!success) {
                    for (int j = 0; j < i; ++j) {
                        this.children[j].shutdown();
                    }
                }
            }
        }
    }

    @Override
    public EventExecutor next() {
        return this.children[Math.abs(this.childIndex.getAndIncrement() % this.children.length)];
    }

    protected /* varargs */ abstract EventExecutor newChild(ThreadFactory var1, ChannelTaskScheduler var2, Object ... var3) throws Exception;

    @Override
    public void shutdown() {
        if (this.isShutdown()) {
            return;
        }
        this.scheduler.shutdown();
        for (EventExecutor l : this.children) {
            l.shutdown();
        }
    }

    @Override
    public boolean isShutdown() {
        if (!this.scheduler.isShutdown()) {
            return false;
        }
        for (EventExecutor l : this.children) {
            if (l.isShutdown()) continue;
            return false;
        }
        return true;
    }

    @Override
    public boolean isTerminated() {
        if (!this.scheduler.isTerminated()) {
            return false;
        }
        for (EventExecutor l : this.children) {
            if (l.isTerminated()) continue;
            return false;
        }
        return true;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long timeLeft;
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        do {
            if ((timeLeft = deadline - System.nanoTime()) > 0) continue;
            return this.isTerminated();
        } while (!this.scheduler.awaitTermination(timeLeft, TimeUnit.NANOSECONDS));
        block1 : for (EventExecutor l : this.children) {
            long timeLeft2;
            while ((timeLeft2 = deadline - System.nanoTime()) > 0) {
                if (!l.awaitTermination(timeLeft2, TimeUnit.NANOSECONDS)) continue;
                continue block1;
            }
            break block1;
        }
        return this.isTerminated();
    }

    private final class DefaultThreadFactory
    implements ThreadFactory {
        private final AtomicInteger nextId;
        private final String prefix;

        DefaultThreadFactory() {
            this.nextId = new AtomicInteger();
            String typeName = MultithreadEventExecutorGroup.this.getClass().getSimpleName();
            typeName = "" + Character.toLowerCase(typeName.charAt(0)) + typeName.substring(1);
            this.prefix = typeName + '-' + poolId.incrementAndGet() + '-';
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, this.prefix + this.nextId.incrementAndGet());
            try {
                if (t.isDaemon()) {
                    t.setDaemon(false);
                }
                if (t.getPriority() != 10) {
                    t.setPriority(10);
                }
            }
            catch (Exception ignored) {
                // empty catch block
            }
            return t;
        }
    }

}

