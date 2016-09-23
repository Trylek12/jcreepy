
package io.netty.handler.logging;

import io.netty.buffer.ChannelBuf;
import io.netty.buffer.MessageBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandler;
import io.netty.channel.ChannelOutboundMessageHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.logging.InternalLogLevel;
import io.netty.logging.InternalLogger;

public class MessageLoggingHandler
extends LoggingHandler
implements ChannelInboundMessageHandler<Object>,
ChannelOutboundMessageHandler<Object> {
    public MessageLoggingHandler() {
    }

    public MessageLoggingHandler(Class<?> clazz, LogLevel level) {
        super(clazz, level);
    }

    public MessageLoggingHandler(Class<?> clazz) {
        super(clazz);
    }

    public MessageLoggingHandler(LogLevel level) {
        super(level);
    }

    public MessageLoggingHandler(String name, LogLevel level) {
        super(name, level);
    }

    public MessageLoggingHandler(String name) {
        super(name);
    }

    @Override
    public MessageBuf<Object> newOutboundBuffer(ChannelHandlerContext ctx) throws Exception {
        return Unpooled.messageBuffer();
    }

    @Override
    public MessageBuf<Object> newInboundBuffer(ChannelHandlerContext ctx) throws Exception {
        return Unpooled.messageBuffer();
    }

    @Override
    public void freeInboundBuffer(ChannelHandlerContext ctx, ChannelBuf buf) throws Exception {
    }

    @Override
    public void freeOutboundBuffer(ChannelHandlerContext ctx, ChannelBuf buf) throws Exception {
    }

    @Override
    public void inboundBufferUpdated(ChannelHandlerContext ctx) throws Exception {
        Object o;
        MessageBuf<Object> buf = ctx.inboundMessageBuffer();
        if (this.logger.isEnabled(this.internalLevel)) {
            this.logger.log(this.internalLevel, this.format(ctx, this.formatBuffer("RECEIVED", buf)));
        }
        MessageBuf<Object> out = ctx.nextInboundMessageBuffer();
        while ((o = buf.poll()) != null) {
            out.add(o);
        }
        ctx.fireInboundBufferUpdated();
    }

    @Override
    public void flush(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
        Object o;
        MessageBuf<Object> buf = ctx.outboundMessageBuffer();
        if (this.logger.isEnabled(this.internalLevel)) {
            this.logger.log(this.internalLevel, this.format(ctx, this.formatBuffer("WRITE", buf)));
        }
        MessageBuf<Object> out = ctx.nextOutboundMessageBuffer();
        while ((o = buf.poll()) != null) {
            out.add(o);
        }
        ctx.flush(future);
    }

    protected String formatBuffer(String message, MessageBuf<Object> buf) {
        return message + '(' + buf.size() + "): " + buf;
    }
}

