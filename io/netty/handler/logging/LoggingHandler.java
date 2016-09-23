
package io.netty.handler.logging;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.logging.LogLevel;
import io.netty.logging.InternalLogLevel;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import java.net.SocketAddress;

@ChannelHandler.Sharable
public class LoggingHandler
extends ChannelHandlerAdapter {
    private static final LogLevel DEFAULT_LEVEL = LogLevel.DEBUG;
    protected final InternalLogger logger;
    protected final InternalLogLevel internalLevel;
    private final LogLevel level;

    public LoggingHandler() {
        this(DEFAULT_LEVEL);
    }

    public LoggingHandler(LogLevel level) {
        if (level == null) {
            throw new NullPointerException("level");
        }
        this.logger = InternalLoggerFactory.getInstance(this.getClass());
        this.level = level;
        this.internalLevel = level.toInternalLevel();
    }

    public LoggingHandler(Class<?> clazz) {
        this(clazz, DEFAULT_LEVEL);
    }

    public LoggingHandler(Class<?> clazz, LogLevel level) {
        if (clazz == null) {
            throw new NullPointerException("clazz");
        }
        if (level == null) {
            throw new NullPointerException("level");
        }
        this.logger = InternalLoggerFactory.getInstance(clazz);
        this.level = level;
        this.internalLevel = level.toInternalLevel();
    }

    public LoggingHandler(String name) {
        this(name, DEFAULT_LEVEL);
    }

    public LoggingHandler(String name, LogLevel level) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (level == null) {
            throw new NullPointerException("level");
        }
        this.logger = InternalLoggerFactory.getInstance(name);
        this.level = level;
        this.internalLevel = level.toInternalLevel();
    }

    public LogLevel level() {
        return this.level;
    }

    protected String format(ChannelHandlerContext ctx, String message) {
        String chStr = ctx.channel().toString();
        StringBuilder buf = new StringBuilder(chStr.length() + message.length() + 1);
        buf.append(chStr);
        buf.append(' ');
        buf.append(message);
        return buf.toString();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        if (this.logger.isEnabled(this.internalLevel)) {
            this.logger.log(this.internalLevel, this.format(ctx, "REGISTERED"));
        }
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        if (this.logger.isEnabled(this.internalLevel)) {
            this.logger.log(this.internalLevel, this.format(ctx, "UNREGISTERED"));
        }
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (this.logger.isEnabled(this.internalLevel)) {
            this.logger.log(this.internalLevel, this.format(ctx, "ACTIVE"));
        }
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (this.logger.isEnabled(this.internalLevel)) {
            this.logger.log(this.internalLevel, this.format(ctx, "INACTIVE"));
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (this.logger.isEnabled(this.internalLevel)) {
            this.logger.log(this.internalLevel, this.format(ctx, "EXCEPTION: " + cause), cause);
        }
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (this.logger.isEnabled(this.internalLevel)) {
            this.logger.log(this.internalLevel, this.format(ctx, "USER_EVENT: " + evt));
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelFuture future) throws Exception {
        if (this.logger.isEnabled(this.internalLevel)) {
            this.logger.log(this.internalLevel, this.format(ctx, "BIND(" + localAddress + ')'));
        }
        super.bind(ctx, localAddress, future);
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelFuture future) throws Exception {
        if (this.logger.isEnabled(this.internalLevel)) {
            this.logger.log(this.internalLevel, this.format(ctx, "CONNECT(" + remoteAddress + ", " + localAddress + ')'));
        }
        super.connect(ctx, remoteAddress, localAddress, future);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
        if (this.logger.isEnabled(this.internalLevel)) {
            this.logger.log(this.internalLevel, this.format(ctx, "DISCONNECT()"));
        }
        super.disconnect(ctx, future);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
        if (this.logger.isEnabled(this.internalLevel)) {
            this.logger.log(this.internalLevel, this.format(ctx, "CLOSE()"));
        }
        super.close(ctx, future);
    }

    @Override
    public void deregister(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
        if (this.logger.isEnabled(this.internalLevel)) {
            this.logger.log(this.internalLevel, this.format(ctx, "DEREGISTER()"));
        }
        super.deregister(ctx, future);
    }

    @Override
    public void flush(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
        ctx.flush(future);
    }

    @Override
    public void inboundBufferUpdated(ChannelHandlerContext ctx) throws Exception {
        ctx.fireInboundBufferUpdated();
    }
}

