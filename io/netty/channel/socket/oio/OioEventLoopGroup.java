
package io.netty.channel.socket.oio;

import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelTaskScheduler;
import io.netty.channel.EventExecutor;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.oio.OioEventLoop;
import java.util.Collections;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class OioEventLoopGroup
implements EventLoopGroup {
    private final int maxChannels;
    final ChannelTaskScheduler scheduler;
    final ThreadFactory threadFactory;
    final Set<OioEventLoop> activeChildren = Collections.newSetFromMap(new ConcurrentHashMap());
    final Queue<OioEventLoop> idleChildren = new ConcurrentLinkedQueue<OioEventLoop>();
    private final ChannelException tooManyChannels;

    public OioEventLoopGroup() {
        this(0);
    }

    public OioEventLoopGroup(int maxChannels) {
        this(maxChannels, Executors.defaultThreadFactory());
    }

    public OioEventLoopGroup(int maxChannels, ThreadFactory threadFactory) {
        if (maxChannels < 0) {
            throw new IllegalArgumentException(String.format("maxChannels: %d (expected: >= 0)", maxChannels));
        }
        if (threadFactory == null) {
            throw new NullPointerException("threadFactory");
        }
        this.maxChannels = maxChannels;
        this.threadFactory = threadFactory;
        this.scheduler = new ChannelTaskScheduler(threadFactory);
        this.tooManyChannels = new ChannelException("too many channels (max: " + maxChannels + ')');
        this.tooManyChannels.setStackTrace(new StackTraceElement[0]);
    }

    @Override
    public EventLoop next() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void shutdown() {
        this.scheduler.shutdown();
        for (EventLoop l2 : this.activeChildren) {
            l2.shutdown();
        }
        for (EventLoop l : this.idleChildren) {
            l.shutdown();
        }
    }

    @Override
    public boolean isShutdown() {
        if (!this.scheduler.isShutdown()) {
            return false;
        }
        for (EventLoop l2 : this.activeChildren) {
            if (l2.isShutdown()) continue;
            return false;
        }
        for (EventLoop l : this.idleChildren) {
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
        for (EventLoop l2 : this.activeChildren) {
            if (l2.isTerminated()) continue;
            return false;
        }
        for (EventLoop l : this.idleChildren) {
            if (l.isTerminated()) continue;
            return false;
        }
        return true;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long timeLeft;
        long timeLeft2;
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        do {
            if ((timeLeft2 = deadline - System.nanoTime()) > 0) continue;
            return this.isTerminated();
        } while (!this.scheduler.awaitTermination(timeLeft2, TimeUnit.NANOSECONDS));
        for (EventLoop l2 : this.activeChildren) {
            do {
                if ((timeLeft = deadline - System.nanoTime()) > 0) continue;
                return this.isTerminated();
            } while (!l2.awaitTermination(timeLeft, TimeUnit.NANOSECONDS));
        }
        for (EventLoop l : this.idleChildren) {
            do {
                if ((timeLeft = deadline - System.nanoTime()) > 0) continue;
                return this.isTerminated();
            } while (!l.awaitTermination(timeLeft, TimeUnit.NANOSECONDS));
        }
        return this.isTerminated();
    }

    @Override
    public ChannelFuture register(Channel channel) {
        if (channel == null) {
            throw new NullPointerException("channel");
        }
        try {
            return this.nextChild().register(channel);
        }
        catch (Throwable t) {
            return channel.newFailedFuture(t);
        }
    }

    @Override
    public ChannelFuture register(Channel channel, ChannelFuture future) {
        if (channel == null) {
            throw new NullPointerException("channel");
        }
        try {
            return this.nextChild().register(channel, future);
        }
        catch (Throwable t) {
            return channel.newFailedFuture(t);
        }
    }

    private EventLoop nextChild() {
        OioEventLoop loop = this.idleChildren.poll();
        if (loop == null) {
            if (this.maxChannels > 0 && this.activeChildren.size() >= this.maxChannels) {
                throw this.tooManyChannels;
            }
            loop = new OioEventLoop(this);
        }
        this.activeChildren.add(loop);
        return loop;
    }
}

