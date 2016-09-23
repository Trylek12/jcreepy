
package io.netty.channel.socket.aio;

import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.aio.AioEventLoop;
import io.netty.channel.socket.aio.AioEventLoopGroup;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannel;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

abstract class AbstractAioChannel
extends AbstractChannel {
    protected final AioEventLoopGroup group;
    private final AsynchronousChannel ch;
    protected ChannelFuture connectFuture;
    protected ScheduledFuture<?> connectTimeoutFuture;
    private ConnectException connectTimeoutException;

    protected AbstractAioChannel(Channel parent, Integer id, AioEventLoopGroup group, AsynchronousChannel ch) {
        super(parent, id);
        this.ch = ch;
        this.group = group;
    }

    @Override
    public InetSocketAddress localAddress() {
        return (InetSocketAddress)super.localAddress();
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return (InetSocketAddress)super.remoteAddress();
    }

    protected AsynchronousChannel javaChannel() {
        return this.ch;
    }

    @Override
    public boolean isOpen() {
        return this.ch.isOpen();
    }

    @Override
    protected Runnable doRegister() throws Exception {
        if (this.eventLoop().parent() != this.group) {
            throw new ChannelException(this.getClass().getSimpleName() + " must be registered to the " + AioEventLoopGroup.class.getSimpleName() + " which was specified in the constructor.");
        }
        return null;
    }

    @Override
    protected void doDeregister() throws Exception {
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return loop instanceof AioEventLoop;
    }

    protected abstract void doConnect(SocketAddress var1, SocketAddress var2, ChannelFuture var3);

    protected abstract class AbstractAioUnsafe
    extends AbstractChannel.AbstractUnsafe {
        protected AbstractAioUnsafe() {
        }

        @Override
        public void connect(final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelFuture future) {
            if (AbstractAioChannel.this.eventLoop().inEventLoop()) {
                if (!this.ensureOpen(future)) {
                    return;
                }
                try {
                    if (AbstractAioChannel.this.connectFuture != null) {
                        throw new IllegalStateException("connection attempt already made");
                    }
                    AbstractAioChannel.this.connectFuture = future;
                    AbstractAioChannel.this.doConnect(remoteAddress, localAddress, future);
                    int connectTimeoutMillis = AbstractAioChannel.this.config().getConnectTimeoutMillis();
                    if (connectTimeoutMillis > 0) {
                        AbstractAioChannel.this.connectTimeoutFuture = AbstractAioChannel.this.eventLoop().schedule(new Runnable(){

                            @Override
                            public void run() {
                                ChannelFuture connectFuture;
                                if (AbstractAioChannel.this.connectTimeoutException == null) {
                                    AbstractAioChannel.this.connectTimeoutException = new ConnectException("connection timed out");
                                }
                                if ((connectFuture = AbstractAioChannel.this.connectFuture) != null && connectFuture.setFailure(AbstractAioChannel.this.connectTimeoutException)) {
                                    AbstractAioChannel.this.pipeline().fireExceptionCaught(AbstractAioChannel.this.connectTimeoutException);
                                    AbstractAioUnsafe.this.close(AbstractAioUnsafe.this.voidFuture());
                                }
                            }
                        }, (long)connectTimeoutMillis, TimeUnit.MILLISECONDS);
                    }
                }
                catch (Throwable t) {
                    future.setFailure(t);
                    this.closeIfClosed();
                }
            } else {
                AbstractAioChannel.this.eventLoop().execute(new Runnable(){

                    @Override
                    public void run() {
                        AbstractAioUnsafe.this.connect(remoteAddress, localAddress, future);
                    }
                });
            }
        }

        protected final void connectFailed(Throwable t) {
            AbstractAioChannel.this.connectFuture.setFailure(t);
            AbstractAioChannel.this.pipeline().fireExceptionCaught(t);
            this.closeIfClosed();
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        protected final void connectSuccess() {
            assert (AbstractAioChannel.this.eventLoop().inEventLoop());
            assert (AbstractAioChannel.this.connectFuture != null);
            try {
                boolean wasActive = AbstractAioChannel.this.isActive();
                AbstractAioChannel.this.connectFuture.setSuccess();
                if (!wasActive && AbstractAioChannel.this.isActive()) {
                    AbstractAioChannel.this.pipeline().fireChannelActive();
                }
            }
            catch (Throwable t) {
                AbstractAioChannel.this.connectFuture.setFailure(t);
                this.closeIfClosed();
            }
            finally {
                AbstractAioChannel.this.connectTimeoutFuture.cancel(false);
                AbstractAioChannel.this.connectFuture = null;
            }
        }

    }

}

