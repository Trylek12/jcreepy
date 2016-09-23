
package io.netty.channel.socket.nio;

import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.nio.NioEventLoop;
import io.netty.channel.socket.nio.NioTask;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class AbstractNioChannel
extends AbstractChannel {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractNioChannel.class);
    private final SelectableChannel ch;
    private final int readInterestOp;
    private volatile SelectionKey selectionKey;
    private volatile boolean inputShutdown;
    final Queue<NioTask<SelectableChannel>> writableTasks = new ConcurrentLinkedQueue<NioTask<SelectableChannel>>();
    final Runnable suspendReadTask;
    final Runnable resumeReadTask;
    private ChannelFuture connectFuture;
    private ScheduledFuture<?> connectTimeoutFuture;
    private ConnectException connectTimeoutException;

    protected AbstractNioChannel(Channel parent, Integer id, SelectableChannel ch, int readInterestOp) {
        super(parent, id);
        this.suspendReadTask = new Runnable(){

            @Override
            public void run() {
                AbstractNioChannel.this.selectionKey().interestOps(AbstractNioChannel.this.selectionKey().interestOps() & ~ AbstractNioChannel.this.readInterestOp);
            }
        };
        this.resumeReadTask = new Runnable(){

            @Override
            public void run() {
                AbstractNioChannel.this.selectionKey().interestOps(AbstractNioChannel.this.selectionKey().interestOps() | AbstractNioChannel.this.readInterestOp);
            }
        };
        this.ch = ch;
        this.readInterestOp = readInterestOp;
        try {
            ch.configureBlocking(false);
        }
        catch (IOException e) {
            block4 : {
                try {
                    ch.close();
                }
                catch (IOException e2) {
                    if (!logger.isWarnEnabled()) break block4;
                    logger.warn("Failed to close a partially initialized socket.", e2);
                }
            }
            throw new ChannelException("Failed to enter non-blocking mode.", e);
        }
    }

    @Override
    public boolean isOpen() {
        return this.ch.isOpen();
    }

    @Override
    public InetSocketAddress localAddress() {
        return (InetSocketAddress)super.localAddress();
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return (InetSocketAddress)super.remoteAddress();
    }

    @Override
    public NioUnsafe unsafe() {
        return (NioUnsafe)super.unsafe();
    }

    protected SelectableChannel javaChannel() {
        return this.ch;
    }

    @Override
    public NioEventLoop eventLoop() {
        return (NioEventLoop)super.eventLoop();
    }

    protected SelectionKey selectionKey() {
        assert (this.selectionKey != null);
        return this.selectionKey;
    }

    boolean isInputShutdown() {
        return this.inputShutdown;
    }

    void setInputShutdown() {
        this.inputShutdown = true;
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return loop instanceof NioEventLoop;
    }

    @Override
    protected boolean isFlushPending() {
        SelectionKey selectionKey = this.selectionKey;
        return selectionKey.isValid() && (selectionKey.interestOps() & 4) != 0;
    }

    @Override
    protected Runnable doRegister() throws Exception {
        NioEventLoop loop = this.eventLoop();
        this.selectionKey = this.javaChannel().register(loop.selector, this.isActive() && !this.inputShutdown ? this.readInterestOp : 0, this);
        return null;
    }

    @Override
    protected void doDeregister() throws Exception {
        this.eventLoop().cancel(this.selectionKey());
    }

    protected abstract boolean doConnect(SocketAddress var1, SocketAddress var2) throws Exception;

    protected abstract void doFinishConnect() throws Exception;

    protected abstract class AbstractNioUnsafe
    extends AbstractChannel.AbstractUnsafe
    implements NioUnsafe {
        protected AbstractNioUnsafe() {
        }

        @Override
        public SelectableChannel ch() {
            return AbstractNioChannel.this.javaChannel();
        }

        @Override
        public void connect(final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelFuture future) {
            if (AbstractNioChannel.this.eventLoop().inEventLoop()) {
                if (!this.ensureOpen(future)) {
                    return;
                }
                try {
                    if (AbstractNioChannel.this.connectFuture != null) {
                        throw new IllegalStateException("connection attempt already made");
                    }
                    boolean wasActive = AbstractNioChannel.this.isActive();
                    if (AbstractNioChannel.this.doConnect(remoteAddress, localAddress)) {
                        future.setSuccess();
                        if (!wasActive && AbstractNioChannel.this.isActive()) {
                            AbstractNioChannel.this.pipeline().fireChannelActive();
                        }
                    } else {
                        AbstractNioChannel.this.connectFuture = future;
                        int connectTimeoutMillis = AbstractNioChannel.this.config().getConnectTimeoutMillis();
                        if (connectTimeoutMillis > 0) {
                            AbstractNioChannel.this.connectTimeoutFuture = AbstractNioChannel.this.eventLoop().schedule(new Runnable(){

                                @Override
                                public void run() {
                                    ChannelFuture connectFuture;
                                    if (AbstractNioChannel.this.connectTimeoutException == null) {
                                        AbstractNioChannel.this.connectTimeoutException = new ConnectException("connection timed out");
                                    }
                                    if ((connectFuture = AbstractNioChannel.this.connectFuture) != null && connectFuture.setFailure(AbstractNioChannel.this.connectTimeoutException)) {
                                        AbstractNioUnsafe.this.close(AbstractNioUnsafe.this.voidFuture());
                                    }
                                }
                            }, (long)connectTimeoutMillis, TimeUnit.MILLISECONDS);
                        }
                    }
                }
                catch (Throwable t) {
                    future.setFailure(t);
                    this.closeIfClosed();
                }
            } else {
                AbstractNioChannel.this.eventLoop().execute(new Runnable(){

                    @Override
                    public void run() {
                        AbstractNioUnsafe.this.connect(remoteAddress, localAddress, future);
                    }
                });
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void finishConnect() {
            assert (AbstractNioChannel.this.eventLoop().inEventLoop());
            assert (AbstractNioChannel.this.connectFuture != null);
            try {
                boolean wasActive = AbstractNioChannel.this.isActive();
                AbstractNioChannel.this.doFinishConnect();
                AbstractNioChannel.this.connectFuture.setSuccess();
                if (!wasActive && AbstractNioChannel.this.isActive()) {
                    AbstractNioChannel.this.pipeline().fireChannelActive();
                }
            }
            catch (Throwable t) {
                AbstractNioChannel.this.connectFuture.setFailure(t);
                this.closeIfClosed();
            }
            finally {
                AbstractNioChannel.this.connectTimeoutFuture.cancel(false);
                AbstractNioChannel.this.connectFuture = null;
            }
        }

        @Override
        public void suspendRead() {
            NioEventLoop loop = AbstractNioChannel.this.eventLoop();
            if (loop.inEventLoop()) {
                AbstractNioChannel.this.suspendReadTask.run();
            } else {
                loop.execute(AbstractNioChannel.this.suspendReadTask);
            }
        }

        @Override
        public void resumeRead() {
            if (AbstractNioChannel.this.inputShutdown) {
                return;
            }
            NioEventLoop loop = AbstractNioChannel.this.eventLoop();
            if (loop.inEventLoop()) {
                AbstractNioChannel.this.resumeReadTask.run();
            } else {
                loop.execute(AbstractNioChannel.this.resumeReadTask);
            }
        }

    }

    public static interface NioUnsafe
    extends Channel.Unsafe {
        public SelectableChannel ch();

        public void finishConnect();

        public void read();
    }

}

