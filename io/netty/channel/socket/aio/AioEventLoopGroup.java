
package io.netty.channel.socket.aio;

import io.netty.channel.ChannelTaskScheduler;
import io.netty.channel.EventExecutor;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopException;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.socket.aio.AbstractAioChannel;
import io.netty.channel.socket.aio.AioChannelFinder;
import io.netty.channel.socket.aio.AioEventLoop;
import io.netty.channel.socket.aio.ReflectiveAioChannelFinder;
import io.netty.channel.socket.aio.UnsafeAioChannelFinder;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import io.netty.util.internal.DetectionUtil;
import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class AioEventLoopGroup
extends MultithreadEventLoopGroup {
    private static final InternalLogger LOGGER;
    private static final AioChannelFinder CHANNEL_FINDER;
    private final AioExecutorService groupExecutor;
    final AsynchronousChannelGroup group;

    public AioEventLoopGroup() {
        this(0);
    }

    public AioEventLoopGroup(int nThreads) {
        this(nThreads, null);
    }

    public AioEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        super(nThreads, threadFactory, new Object[0]);
        this.groupExecutor = new AioExecutorService();
        try {
            this.group = AsynchronousChannelGroup.withThreadPool(this.groupExecutor);
        }
        catch (IOException e) {
            throw new EventLoopException("Failed to create an AsynchronousChannelGroup", e);
        }
    }

    @Override
    public void shutdown() {
        boolean interrupted = false;
        try {
            this.group.shutdownNow();
        }
        catch (IOException e) {
            throw new EventLoopException("failed to shut down a channel group", e);
        }
        while (!this.groupExecutor.isTerminated()) {
            try {
                this.groupExecutor.awaitTermination(1, TimeUnit.HOURS);
            }
            catch (InterruptedException e) {
                interrupted = true;
            }
        }
        super.shutdown();
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    protected /* varargs */ EventExecutor newChild(ThreadFactory threadFactory, ChannelTaskScheduler scheduler, Object ... args) throws Exception {
        return new AioEventLoop(this, threadFactory, scheduler);
    }

    static {
        AioChannelFinder finder2;
        AioChannelFinder finder2;
        LOGGER = InternalLoggerFactory.getInstance(AioEventLoopGroup.class);
        try {
            finder2 = DetectionUtil.hasUnsafe() ? new UnsafeAioChannelFinder() : new ReflectiveAioChannelFinder();
        }
        catch (Throwable t) {
            LOGGER.debug(String.format("Failed to instantiate the optimal %s implementation - falling back to %s.", AioChannelFinder.class.getSimpleName(), ReflectiveAioChannelFinder.class.getSimpleName()), t);
            finder2 = new ReflectiveAioChannelFinder();
        }
        CHANNEL_FINDER = finder2;
    }

    private final class AioExecutorService
    extends AbstractExecutorService {
        private final CountDownLatch latch;

        private AioExecutorService() {
            this.latch = new CountDownLatch(1);
        }

        @Override
        public void shutdown() {
            this.latch.countDown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            this.shutdown();
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return this.latch.getCount() == 0;
        }

        @Override
        public boolean isTerminated() {
            return this.isShutdown();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return this.latch.await(timeout, unit);
        }

        @Override
        public void execute(Runnable command) {
            Class commandType = command.getClass();
            if (commandType.getName().startsWith("sun.nio.ch.")) {
                this.executeAioTask(command);
            } else {
                AioEventLoopGroup.this.next().execute(command);
            }
        }

        private void executeAioTask(Runnable command) {
            AbstractAioChannel ch = null;
            try {
                ch = CHANNEL_FINDER.findChannel(command);
            }
            catch (Throwable t) {
                // empty catch block
            }
            EventLoop l = ch != null ? ch.eventLoop() : AioEventLoopGroup.this.next();
            if (l.isShutdown()) {
                command.run();
            } else {
                l.execute(command);
            }
        }
    }

}

