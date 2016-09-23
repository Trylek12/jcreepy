
package io.netty.channel.socket.nio;

import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelTaskScheduler;
import io.netty.channel.EventLoopException;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.channel.socket.nio.AbstractNioChannel;
import io.netty.channel.socket.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioTask;
import io.netty.channel.socket.nio.SelectorUtil;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

public final class NioEventLoop
extends SingleThreadEventLoop {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NioEventLoop.class);
    static final int CLEANUP_INTERVAL = 256;
    Selector selector;
    private final SelectorProvider provider;
    private final AtomicBoolean wakenUp = new AtomicBoolean();
    private int cancelledKeys;
    private boolean cleanedCancelledKeys;

    NioEventLoop(NioEventLoopGroup parent, ThreadFactory threadFactory, ChannelTaskScheduler scheduler, SelectorProvider selectorProvider) {
        super(parent, threadFactory, scheduler);
        if (selectorProvider == null) {
            throw new NullPointerException("selectorProvider");
        }
        this.provider = selectorProvider;
        this.selector = this.openSelector();
    }

    private Selector openSelector() {
        try {
            return this.provider.openSelector();
        }
        catch (IOException e) {
            throw new ChannelException("failed to open a new selector", e);
        }
    }

    @Override
    protected Queue<Runnable> newTaskQueue() {
        return new ConcurrentLinkedQueue<Runnable>();
    }

    public void register(SelectableChannel ch, int interestOps, NioTask<?> task) {
        if (ch == null) {
            throw new NullPointerException("ch");
        }
        if (interestOps == 0) {
            throw new IllegalArgumentException("interestOps must be non-zero.");
        }
        if ((interestOps & ~ ch.validOps()) != 0) {
            throw new IllegalArgumentException("invalid interestOps: " + interestOps + "(validOps: " + ch.validOps() + ')');
        }
        if (task == null) {
            throw new NullPointerException("task");
        }
        if (this.isShutdown()) {
            throw new IllegalStateException("event loop shut down");
        }
        try {
            ch.register(this.selector, interestOps, task);
        }
        catch (Exception e) {
            throw new EventLoopException("failed to register a channel", e);
        }
    }

    void executeWhenWritable(AbstractNioChannel channel, NioTask<? extends SelectableChannel> task) {
        if (channel == null) {
            throw new NullPointerException("channel");
        }
        if (this.isShutdown()) {
            throw new IllegalStateException("event loop shut down");
        }
        SelectionKey key = channel.selectionKey();
        channel.writableTasks.offer(task);
        key.interestOps(key.interestOps() | 4);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private Selector recreateSelector() {
        Selector newSelector = this.openSelector();
        Selector oldSelector = this.selector;
        boolean success = false;
        try {
            for (SelectionKey key : oldSelector.keys()) {
                key.channel().register(newSelector, key.interestOps(), key.attachment());
            }
            success = true;
        }
        catch (Exception e) {
            logger.warn("Failed to re-register a Channel to the new Selector.", e);
        }
        finally {
            if (!success) {
                try {
                    newSelector.close();
                }
                catch (Exception e) {
                    logger.warn("Failed to close the new Selector.", e);
                }
            }
        }
        if (!success) {
            return oldSelector;
        }
        try {
            this.selector.close();
        }
        catch (Exception e) {
            logger.warn("Failed to close the old selector.", e);
        }
        logger.info("Selector migration complete.");
        this.selector = newSelector;
        return this.selector;
    }

    @Override
    protected void run() {
        Selector selector = this.selector;
        int selectReturnsImmediately = 0;
        long minSelectTimeout = SelectorUtil.SELECT_TIMEOUT_NANOS / 100 * 80;
        do {
            this.wakenUp.set(false);
            try {
                long beforeSelect = System.nanoTime();
                int selected = SelectorUtil.select(selector);
                if (SelectorUtil.EPOLL_BUG_WORKAROUND) {
                    if (selected == 0) {
                        long timeBlocked = System.nanoTime() - beforeSelect;
                        selectReturnsImmediately = timeBlocked < minSelectTimeout ? ++selectReturnsImmediately : 0;
                        if (selectReturnsImmediately == 10) {
                            selector = this.recreateSelector();
                            selectReturnsImmediately = 0;
                            continue;
                        }
                    } else {
                        selectReturnsImmediately = 0;
                    }
                }
                if (this.wakenUp.get()) {
                    selector.wakeup();
                }
                this.cancelledKeys = 0;
                this.runAllTasks();
                this.processSelectedKeys();
                if (!this.isShutdown()) continue;
                this.closeAll();
                if (!this.confirmShutdown()) continue;
                break;
            }
            catch (Throwable t) {
                logger.warn("Unexpected exception in the selector loop.", t);
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {}
                continue;
            }
            break;
        } while (true);
    }

    @Override
    protected void cleanup() {
        try {
            this.selector.close();
        }
        catch (IOException e) {
            logger.warn("Failed to close a selector.", e);
        }
    }

    void cancel(SelectionKey key) {
        key.cancel();
        ++this.cancelledKeys;
        if (this.cancelledKeys >= 256) {
            this.cancelledKeys = 0;
            this.cleanedCancelledKeys = true;
            SelectorUtil.cleanupKeys(this.selector);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void processSelectedKeys() {
        Set<SelectionKey> selectedKeys = this.selector.selectedKeys();
        if (selectedKeys.isEmpty()) {
            return;
        }
        this.cleanedCancelledKeys = false;
        boolean clearSelectedKeys = true;
        try {
            Iterator<SelectionKey> i = selectedKeys.iterator();
            while (i.hasNext()) {
                SelectionKey k = i.next();
                Object a = k.attachment();
                if (a instanceof AbstractNioChannel) {
                    NioEventLoop.processSelectedKey(k, (AbstractNioChannel)a);
                } else {
                    NioTask task = (NioTask)a;
                    NioEventLoop.processSelectedKey(k, task);
                }
                if (!this.cleanedCancelledKeys) continue;
                if (selectedKeys.isEmpty()) {
                    clearSelectedKeys = false;
                    break;
                }
                i = selectedKeys.iterator();
            }
        }
        finally {
            if (clearSelectedKeys) {
                selectedKeys.clear();
            }
        }
    }

    private static void processSelectedKey(SelectionKey k, AbstractNioChannel ch) {
        AbstractNioChannel.NioUnsafe unsafe = ch.unsafe();
        int readyOps = -1;
        try {
            readyOps = k.readyOps();
            if ((readyOps & 17) != 0 || readyOps == 0) {
                unsafe.read();
                if (!ch.isOpen()) {
                    return;
                }
            }
            if ((readyOps & 4) != 0) {
                NioEventLoop.processWritable(k, ch);
            }
            if ((readyOps & 8) != 0) {
                unsafe.finishConnect();
            }
        }
        catch (CancelledKeyException e) {
            if (readyOps != 1 && (readyOps & 4) != 0) {
                NioEventLoop.unregisterWritableTasks(ch);
            }
            unsafe.close(unsafe.voidFuture());
        }
    }

    private static void processWritable(SelectionKey k, AbstractNioChannel ch) {
        if (ch.writableTasks.isEmpty()) {
            ch.unsafe().flushNow();
        } else {
            NioTask<SelectableChannel> task = null;
            while ((task = ch.writableTasks.poll()) != null) {
                NioEventLoop.processSelectedKey(ch.selectionKey(), task);
            }
            k.interestOps(k.interestOps() | 4);
        }
    }

    private static void unregisterWritableTasks(AbstractNioChannel ch) {
        NioTask<SelectableChannel> task = null;
        while ((task = ch.writableTasks.poll()) != null) {
            NioEventLoop.invokeChannelUnregistered(task, ch.selectionKey());
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void processSelectedKey(SelectionKey k, NioTask<SelectableChannel> task) {
        boolean success = false;
        try {
            task.channelReady(k.channel(), k);
            success = true;
        }
        catch (Exception e) {
            logger.warn("Unexpected exception while running NioTask.channelReady() - cancelling the key", e);
        }
        finally {
            if (!success) {
                k.cancel();
            }
            if (!k.isValid()) {
                NioEventLoop.invokeChannelUnregistered(task, k);
            }
        }
    }

    private void closeAll() {
        SelectorUtil.cleanupKeys(this.selector);
        Set<SelectionKey> keys = this.selector.keys();
        ArrayList<AbstractNioChannel> channels = new ArrayList<AbstractNioChannel>(keys.size());
        for (SelectionKey k : keys) {
            Object a = k.attachment();
            if (a instanceof AbstractNioChannel) {
                channels.add((AbstractNioChannel)a);
                continue;
            }
            k.cancel();
            NioTask task = (NioTask)a;
            NioEventLoop.invokeChannelUnregistered(task, k);
        }
        for (AbstractNioChannel ch : channels) {
            NioEventLoop.unregisterWritableTasks(ch);
            ch.unsafe().close(ch.unsafe().voidFuture());
        }
    }

    private static void invokeChannelUnregistered(NioTask<SelectableChannel> task, SelectionKey k) {
        try {
            task.channelUnregistered(k.channel());
        }
        catch (Exception e) {
            logger.warn("Unexpected exception while running NioTask.channelUnregistered()", e);
        }
    }

    @Override
    protected void wakeup(boolean inEventLoop) {
        if (this.wakenUp.compareAndSet(false, true)) {
            this.selector.wakeup();
        }
    }
}

