
package io.netty.channel;

import io.netty.buffer.ChannelBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOperationHandler;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelStateHandler;
import java.net.SocketAddress;

public class CombinedChannelHandler
extends ChannelHandlerAdapter
implements ChannelInboundHandler,
ChannelOutboundHandler {
    private ChannelOutboundHandler out;
    private ChannelInboundHandler in;

    protected CombinedChannelHandler() {
    }

    public CombinedChannelHandler(ChannelInboundHandler inboundHandler, ChannelOutboundHandler outboundHandler) {
        this.init(inboundHandler, outboundHandler);
    }

    protected void init(ChannelInboundHandler inboundHandler, ChannelOutboundHandler outboundHandler) {
        if (inboundHandler == null) {
            throw new NullPointerException("inboundHandler");
        }
        if (outboundHandler == null) {
            throw new NullPointerException("outboundHandler");
        }
        if (inboundHandler instanceof ChannelOperationHandler) {
            throw new IllegalArgumentException("inboundHandler must not implement " + ChannelOperationHandler.class.getSimpleName() + " to get combined.");
        }
        if (outboundHandler instanceof ChannelStateHandler) {
            throw new IllegalArgumentException("outboundHandler must not implement " + ChannelStateHandler.class.getSimpleName() + " to get combined.");
        }
        if (this.in != null) {
            throw new IllegalStateException("init() cannot be called more than once.");
        }
        this.in = inboundHandler;
        this.out = outboundHandler;
    }

    @Override
    public ChannelBuf newInboundBuffer(ChannelHandlerContext ctx) throws Exception {
        return this.in.newInboundBuffer(ctx);
    }

    @Override
    public void freeInboundBuffer(ChannelHandlerContext ctx, ChannelBuf buf) throws Exception {
        this.in.freeInboundBuffer(ctx, buf);
    }

    @Override
    public ChannelBuf newOutboundBuffer(ChannelHandlerContext ctx) throws Exception {
        return this.out.newOutboundBuffer(ctx);
    }

    @Override
    public void freeOutboundBuffer(ChannelHandlerContext ctx, ChannelBuf buf) throws Exception {
        this.out.freeOutboundBuffer(ctx, buf);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void beforeAdd(ChannelHandlerContext ctx) throws Exception {
        if (this.in == null) {
            throw new IllegalStateException("not initialized yet - call init() in the constructor of the subclass");
        }
        try {
            this.in.beforeAdd(ctx);
        }
        finally {
            this.out.beforeAdd(ctx);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void afterAdd(ChannelHandlerContext ctx) throws Exception {
        try {
            this.in.afterAdd(ctx);
        }
        finally {
            this.out.afterAdd(ctx);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void beforeRemove(ChannelHandlerContext ctx) throws Exception {
        try {
            this.in.beforeRemove(ctx);
        }
        finally {
            this.out.beforeRemove(ctx);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void afterRemove(ChannelHandlerContext ctx) throws Exception {
        try {
            this.in.afterRemove(ctx);
        }
        finally {
            this.out.afterRemove(ctx);
        }
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        this.in.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        this.in.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.in.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.in.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        this.in.exceptionCaught(ctx, cause);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        this.in.userEventTriggered(ctx, evt);
    }

    @Override
    public void inboundBufferUpdated(ChannelHandlerContext ctx) throws Exception {
        this.in.inboundBufferUpdated(ctx);
    }

    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelFuture future) throws Exception {
        this.out.bind(ctx, localAddress, future);
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelFuture future) throws Exception {
        this.out.connect(ctx, remoteAddress, localAddress, future);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
        this.out.disconnect(ctx, future);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
        this.out.close(ctx, future);
    }

    @Override
    public void deregister(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
        this.out.deregister(ctx, future);
    }

    @Override
    public void flush(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
        this.out.flush(ctx, future);
    }
}

