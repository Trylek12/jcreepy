
package io.netty.channel.socket.aio;

import io.netty.buffer.ChannelBufType;
import io.netty.buffer.MessageBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.ServerSocketChannelConfig;
import io.netty.channel.socket.aio.AbstractAioChannel;
import io.netty.channel.socket.aio.AioCompletionHandler;
import io.netty.channel.socket.aio.AioEventLoopGroup;
import io.netty.channel.socket.aio.AioServerSocketChannelConfig;
import io.netty.channel.socket.aio.AioSocketChannel;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannel;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.atomic.AtomicBoolean;

public class AioServerSocketChannel
extends AbstractAioChannel
implements ServerSocketChannel {
    private static final ChannelMetadata METADATA = new ChannelMetadata(ChannelBufType.MESSAGE, false);
    private static final AcceptHandler ACCEPT_HANDLER = new AcceptHandler();
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AioServerSocketChannel.class);
    private final AioEventLoopGroup childGroup;
    private final AioServerSocketChannelConfig config;
    private boolean closed;
    private final AtomicBoolean readSuspended = new AtomicBoolean();
    private final Runnable acceptTask;

    private static AsynchronousServerSocketChannel newSocket(AsynchronousChannelGroup group) {
        try {
            return AsynchronousServerSocketChannel.open(group);
        }
        catch (IOException e) {
            throw new ChannelException("Failed to open a socket.", e);
        }
    }

    public AioServerSocketChannel(AioEventLoopGroup group) {
        this(group, group);
    }

    public AioServerSocketChannel(AioEventLoopGroup parentGroup, AioEventLoopGroup childGroup) {
        super(null, null, parentGroup, AioServerSocketChannel.newSocket(parentGroup.group));
        this.acceptTask = new Runnable(){

            @Override
            public void run() {
                AioServerSocketChannel.this.doAccept();
            }
        };
        this.childGroup = childGroup;
        this.config = new AioServerSocketChannelConfig(this.javaChannel());
    }

    @Override
    protected AsynchronousServerSocketChannel javaChannel() {
        return (AsynchronousServerSocketChannel)super.javaChannel();
    }

    @Override
    public boolean isActive() {
        return this.javaChannel().isOpen() && this.localAddress0() != null;
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    protected SocketAddress localAddress0() {
        try {
            return this.javaChannel().getLocalAddress();
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return null;
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        AsynchronousServerSocketChannel ch = this.javaChannel();
        ch.bind(localAddress, this.config.getBacklog());
        this.doAccept();
    }

    private void doAccept() {
        if (this.readSuspended.get()) {
            return;
        }
        this.javaChannel().accept(this, ACCEPT_HANDLER);
    }

    @Override
    protected void doClose() throws Exception {
        if (!this.closed) {
            this.closed = true;
            this.javaChannel().close();
        }
    }

    @Override
    protected boolean isFlushPending() {
        return false;
    }

    @Override
    protected void doConnect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelFuture future) {
        future.setFailure(new UnsupportedOperationException());
    }

    @Override
    protected void doDisconnect() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Runnable doRegister() throws Exception {
        return super.doRegister();
    }

    @Override
    public ServerSocketChannelConfig config() {
        return this.config;
    }

    @Override
    protected Channel.Unsafe newUnsafe() {
        return new AioServerSocketUnsafe();
    }

    private final class AioServerSocketUnsafe
    extends AbstractAioChannel.AbstractAioUnsafe {
        private AioServerSocketUnsafe() {
        }

        @Override
        public void suspendRead() {
            AioServerSocketChannel.this.readSuspended.set(true);
        }

        @Override
        public void resumeRead() {
            if (AioServerSocketChannel.this.readSuspended.compareAndSet(true, false)) {
                if (AioServerSocketChannel.this.eventLoop().inEventLoop()) {
                    AioServerSocketChannel.this.doAccept();
                } else {
                    AioServerSocketChannel.this.eventLoop().execute(AioServerSocketChannel.this.acceptTask);
                }
            }
        }
    }

    private static final class AcceptHandler
    extends AioCompletionHandler<AsynchronousSocketChannel, AioServerSocketChannel> {
        private AcceptHandler() {
        }

        @Override
        protected void completed0(AsynchronousSocketChannel ch, AioServerSocketChannel channel) {
            channel.doAccept();
            channel.pipeline().inboundMessageBuffer().add((AioSocketChannel)new AioSocketChannel(channel, null, channel.childGroup, ch));
            channel.pipeline().fireInboundBufferUpdated();
        }

        @Override
        protected void failed0(Throwable t, AioServerSocketChannel channel) {
            boolean asyncClosed = false;
            if (t instanceof AsynchronousCloseException) {
                asyncClosed = true;
                channel.closed = true;
            }
            if (channel.isOpen() && !asyncClosed) {
                logger.warn("Failed to create a new channel from an accepted socket.", t);
            }
        }
    }

}

