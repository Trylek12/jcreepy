
package io.netty.channel;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelStateHandler;
import java.lang.annotation.Annotation;

public class ChannelStateHandlerAdapter
implements ChannelStateHandler {
    boolean added;

    final boolean isSharable() {
        return this.getClass().isAnnotationPresent(ChannelHandler.Sharable.class);
    }

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
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelRegistered();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelUnregistered();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
    }

    @Override
    public void inboundBufferUpdated(ChannelHandlerContext ctx) throws Exception {
        if (this instanceof ChannelInboundHandler) {
            throw new IllegalStateException("inboundBufferUpdated(...) must be overridden by " + this.getClass().getName() + ", which implements " + ChannelInboundHandler.class.getSimpleName());
        }
        ctx.fireInboundBufferUpdated();
    }
}

