
package io.netty.bootstrap;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.buffer.ChannelBuf;
import io.netty.buffer.MessageBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInboundMessageHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.aio.AioEventLoopGroup;
import io.netty.channel.socket.aio.AioServerSocketChannel;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.NetworkConstants;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ServerBootstrap
extends AbstractBootstrap<ServerBootstrap> {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ServerBootstrap.class);
    private static final InetSocketAddress DEFAULT_LOCAL_ADDR = new InetSocketAddress(NetworkConstants.LOCALHOST, 0);
    private final ChannelHandler acceptor;
    private final Map<ChannelOption<?>, Object> childOptions;
    private final Map<AttributeKey<?>, Object> childAttrs;
    private EventLoopGroup childGroup;
    private ChannelHandler childHandler;

    public ServerBootstrap() {
        this.acceptor = new ChannelInitializer<Channel>(){

            @Override
            public void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast(new Acceptor());
            }
        };
        this.childOptions = new LinkedHashMap();
        this.childAttrs = new LinkedHashMap();
    }

    @Override
    public ServerBootstrap group(EventLoopGroup group) {
        return this.group(group, group);
    }

    public ServerBootstrap group(EventLoopGroup parentGroup, EventLoopGroup childGroup) {
        super.group(parentGroup);
        if (childGroup == null) {
            throw new NullPointerException("childGroup");
        }
        if (this.childGroup != null) {
            throw new IllegalStateException("childGroup set already");
        }
        this.childGroup = childGroup;
        return this;
    }

    @Override
    public ServerBootstrap channel(Class<? extends Channel> channelClass) {
        if (channelClass == null) {
            throw new NullPointerException("channelClass");
        }
        if (!ServerChannel.class.isAssignableFrom(channelClass)) {
            throw new IllegalArgumentException("channelClass must be subtype of " + ServerChannel.class.getSimpleName() + '.');
        }
        if (channelClass == AioServerSocketChannel.class) {
            return (ServerBootstrap)this.channelFactory(new AioServerSocketChannelFactory());
        }
        return (ServerBootstrap)super.channel(channelClass);
    }

    public <T> ServerBootstrap childOption(ChannelOption<T> childOption, T value) {
        if (childOption == null) {
            throw new NullPointerException("childOption");
        }
        if (value == null) {
            this.childOptions.remove(childOption);
        } else {
            this.childOptions.put(childOption, value);
        }
        return this;
    }

    public <T> ServerBootstrap childAttr(AttributeKey<T> childKey, T value) {
        if (childKey == null) {
            throw new NullPointerException("childKey");
        }
        if (value == null) {
            this.childAttrs.remove(childKey);
        } else {
            this.childAttrs.put(childKey, value);
        }
        return this;
    }

    public ServerBootstrap childHandler(ChannelHandler childHandler) {
        if (childHandler == null) {
            throw new NullPointerException("childHandler");
        }
        this.childHandler = childHandler;
        return this;
    }

    @Override
    public ChannelFuture bind(ChannelFuture future) {
        this.validate(future);
        Channel channel = future.channel();
        if (channel.isActive()) {
            future.setFailure(new IllegalStateException("channel already bound: " + channel));
            return future;
        }
        if (channel.isRegistered()) {
            future.setFailure(new IllegalStateException("channel already registered: " + channel));
            return future;
        }
        if (!channel.isOpen()) {
            future.setFailure(new ClosedChannelException());
            return future;
        }
        try {
            channel.config().setOptions(this.options());
        }
        catch (Exception e) {
            future.setFailure(e);
            return future;
        }
        for (Map.Entry e : this.attrs().entrySet()) {
            AttributeKey key = e.getKey();
            channel.attr(key).set((Object)e.getValue());
        }
        ChannelPipeline p = future.channel().pipeline();
        if (this.handler() != null) {
            p.addLast(this.handler());
        }
        p.addLast(this.acceptor);
        ChannelFuture f = this.group().register(channel).awaitUninterruptibly();
        if (!f.isSuccess()) {
            future.setFailure(f.cause());
            return future;
        }
        if (!ServerBootstrap.ensureOpen(future)) {
            return future;
        }
        channel.bind(this.localAddress(), future).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        return future;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if (this.childGroup != null) {
            this.childGroup.shutdown();
        }
    }

    @Override
    protected void validate() {
        super.validate();
        if (this.childHandler == null) {
            throw new IllegalStateException("childHandler not set");
        }
        if (this.childGroup == null) {
            logger.warn("childGroup is not set. Using parentGroup instead.");
            this.childGroup = this.group();
        }
        if (this.localAddress() == null) {
            logger.warn("localAddress is not set. Using " + DEFAULT_LOCAL_ADDR + " instead.");
            this.localAddress(DEFAULT_LOCAL_ADDR);
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(super.toString());
        buf.setLength(buf.length() - 1);
        buf.append(", ");
        if (this.childGroup != null) {
            buf.append("childGroup: ");
            buf.append(this.childGroup.getClass().getSimpleName());
            buf.append(", ");
        }
        if (this.childOptions != null && !this.childOptions.isEmpty()) {
            buf.append("childOptions: ");
            buf.append(this.childOptions);
            buf.append(", ");
        }
        if (this.childAttrs != null && !this.childAttrs.isEmpty()) {
            buf.append("childAttrs: ");
            buf.append(this.childAttrs);
            buf.append(", ");
        }
        if (this.childHandler != null) {
            buf.append("childHandler: ");
            buf.append(this.childHandler);
            buf.append(", ");
        }
        if (buf.charAt(buf.length() - 1) == '(') {
            buf.append(')');
        } else {
            buf.setCharAt(buf.length() - 2, ')');
            buf.setLength(buf.length() - 1);
        }
        return buf.toString();
    }

    private final class AioServerSocketChannelFactory
    implements AbstractBootstrap.ChannelFactory {
        private AioServerSocketChannelFactory() {
        }

        @Override
        public Channel newChannel() {
            return new AioServerSocketChannel((AioEventLoopGroup)ServerBootstrap.this.group(), (AioEventLoopGroup)ServerBootstrap.this.childGroup);
        }

        public String toString() {
            return AioServerSocketChannel.class.getSimpleName() + ".class";
        }
    }

    private class Acceptor
    extends ChannelInboundHandlerAdapter
    implements ChannelInboundMessageHandler<Channel> {
        private Acceptor() {
        }

        @Override
        public MessageBuf<Channel> newInboundBuffer(ChannelHandlerContext ctx) throws Exception {
            return Unpooled.messageBuffer();
        }

        @Override
        public void inboundBufferUpdated(ChannelHandlerContext ctx) {
            Channel child;
            MessageBuf in = ctx.inboundMessageBuffer();
            while ((child = (Channel)in.poll()) != null) {
                child.pipeline().addLast(ServerBootstrap.this.childHandler);
                for (Map.Entry e2 : ServerBootstrap.this.childOptions.entrySet()) {
                    try {
                        if (child.config().setOption((ChannelOption)e2.getKey(), e2.getValue())) continue;
                        logger.warn("Unknown channel option: " + e2);
                    }
                    catch (Throwable t) {
                        logger.warn("Failed to set a channel option: " + child, t);
                    }
                }
                for (Map.Entry e2 : ServerBootstrap.this.childAttrs.entrySet()) {
                    child.attr((AttributeKey)e2.getKey()).set(e2.getValue());
                }
                try {
                    ServerBootstrap.this.childGroup.register(child);
                }
                catch (Throwable t) {
                    child.unsafe().closeForcibly();
                    logger.warn("Failed to register an accepted channel: " + child, t);
                }
            }
        }
    }

}

