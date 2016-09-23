
package io.netty.channel.embedded;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ChannelBuf;
import io.netty.buffer.MessageBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.EventLoop;
import io.netty.channel.embedded.EmbeddedEventLoop;
import io.netty.channel.embedded.EmbeddedSocketAddress;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import java.net.SocketAddress;

public abstract class AbstractEmbeddedChannel
extends AbstractChannel {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractEmbeddedChannel.class);
    private final ChannelConfig config = new DefaultChannelConfig();
    private final SocketAddress localAddress = new EmbeddedSocketAddress();
    private final SocketAddress remoteAddress = new EmbeddedSocketAddress();
    private final MessageBuf<Object> lastInboundMessageBuffer = Unpooled.messageBuffer();
    private final ByteBuf lastInboundByteBuffer = Unpooled.buffer();
    protected final Object lastOutboundBuffer;
    private Throwable lastException;
    private int state;

    /* varargs */ AbstractEmbeddedChannel(Object lastOutboundBuffer, ChannelHandler ... handlers) {
        super(null, null);
        if (handlers == null) {
            throw new NullPointerException("handlers");
        }
        this.lastOutboundBuffer = lastOutboundBuffer;
        int nHandlers = 0;
        boolean hasBuffer = false;
        ChannelPipeline p = this.pipeline();
        for (ChannelHandler h : handlers) {
            if (h == null) break;
            ++nHandlers;
            p.addLast(h);
            if (!(h instanceof ChannelInboundHandler) && !(h instanceof ChannelOutboundHandler)) continue;
            hasBuffer = true;
        }
        if (nHandlers == 0) {
            throw new IllegalArgumentException("handlers is empty.");
        }
        if (!hasBuffer) {
            throw new IllegalArgumentException("handlers does not provide any buffers.");
        }
        p.addLast(new LastInboundMessageHandler(), new LastInboundByteHandler());
        new EmbeddedEventLoop().register(this);
    }

    @Override
    public ChannelConfig config() {
        return this.config;
    }

    @Override
    public boolean isOpen() {
        return this.state < 2;
    }

    @Override
    public boolean isActive() {
        return this.state == 1;
    }

    public MessageBuf<Object> lastInboundMessageBuffer() {
        return this.lastInboundMessageBuffer;
    }

    public ByteBuf lastInboundByteBuffer() {
        return this.lastInboundByteBuffer;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public Object readInbound() {
        if (this.lastInboundByteBuffer.readable()) {
            try {
                ByteBuf byteBuf = this.lastInboundByteBuffer.readBytes(this.lastInboundByteBuffer.readableBytes());
                return byteBuf;
            }
            finally {
                this.lastInboundByteBuffer.clear();
            }
        }
        return this.lastInboundMessageBuffer.poll();
    }

    public void checkException() {
        Throwable t = this.lastException;
        if (t == null) {
            return;
        }
        this.lastException = null;
        if (t instanceof RuntimeException) {
            throw (RuntimeException)t;
        }
        if (t instanceof Error) {
            throw (Error)t;
        }
        throw new ChannelException(t);
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return loop instanceof EmbeddedEventLoop;
    }

    @Override
    protected SocketAddress localAddress0() {
        return this.isActive() ? this.localAddress : null;
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return this.isActive() ? this.remoteAddress : null;
    }

    @Override
    protected Runnable doRegister() throws Exception {
        this.state = 1;
        return null;
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
    }

    @Override
    protected void doDisconnect() throws Exception {
        this.doClose();
    }

    @Override
    protected void doClose() throws Exception {
        this.state = 2;
    }

    @Override
    protected void doDeregister() throws Exception {
    }

    @Override
    protected Channel.Unsafe newUnsafe() {
        return new DefaultUnsafe();
    }

    @Override
    protected boolean isFlushPending() {
        return false;
    }

    private final class LastInboundByteHandler
    extends ChannelInboundHandlerAdapter {
        private LastInboundByteHandler() {
        }

        @Override
        public ChannelBuf newInboundBuffer(ChannelHandlerContext ctx) throws Exception {
            return AbstractEmbeddedChannel.this.lastInboundByteBuffer;
        }

        @Override
        public void inboundBufferUpdated(ChannelHandlerContext ctx) throws Exception {
        }
    }

    private final class LastInboundMessageHandler
    extends ChannelInboundHandlerAdapter {
        private LastInboundMessageHandler() {
        }

        @Override
        public ChannelBuf newInboundBuffer(ChannelHandlerContext ctx) throws Exception {
            return AbstractEmbeddedChannel.this.lastInboundMessageBuffer;
        }

        @Override
        public void inboundBufferUpdated(ChannelHandlerContext ctx) throws Exception {
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            if (AbstractEmbeddedChannel.this.lastException == null) {
                AbstractEmbeddedChannel.this.lastException = cause;
            } else {
                logger.warn("More than one exception was raised. Will report only the first one and log others.", cause);
            }
        }
    }

    private class DefaultUnsafe
    extends AbstractChannel.AbstractUnsafe {
        private DefaultUnsafe() {
        }

        @Override
        public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelFuture future) {
            future.setSuccess();
        }

        @Override
        public void suspendRead() {
        }

        @Override
        public void resumeRead() {
        }
    }

}

