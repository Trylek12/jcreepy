
package io.netty.channel;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOperationHandler;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.FileRegion;
import java.net.SocketAddress;

public class ChannelOperationHandlerAdapter
implements ChannelOperationHandler {
    @Override
    public void beforeAdd(ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    public void afterAdd(ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    public void beforeRemove(ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    public void afterRemove(ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.fireExceptionCaught(cause);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelFuture future) throws Exception {
        ctx.bind(localAddress, future);
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelFuture future) throws Exception {
        ctx.connect(remoteAddress, localAddress, future);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
        ctx.disconnect(future);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
        ctx.close(future);
    }

    @Override
    public void deregister(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
        ctx.deregister(future);
    }

    @Override
    public void flush(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
        if (this instanceof ChannelOutboundHandler) {
            throw new IllegalStateException("flush(...) must be overridden by " + this.getClass().getName() + ", which implements " + ChannelOutboundHandler.class.getSimpleName());
        }
        ctx.flush(future);
    }

    @Override
    public void sendFile(ChannelHandlerContext ctx, FileRegion region, ChannelFuture future) throws Exception {
        ctx.sendFile(region, future);
    }
}

