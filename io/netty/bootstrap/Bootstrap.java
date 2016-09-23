
package io.netty.bootstrap;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.aio.AioEventLoopGroup;
import io.netty.channel.socket.aio.AioSocketChannel;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.Map;
import java.util.Set;

public class Bootstrap
extends AbstractBootstrap<Bootstrap> {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(Bootstrap.class);
    private SocketAddress remoteAddress;

    public Bootstrap remoteAddress(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
        return this;
    }

    public Bootstrap remoteAddress(String host, int port) {
        this.remoteAddress = new InetSocketAddress(host, port);
        return this;
    }

    public Bootstrap remoteAddress(InetAddress host, int port) {
        this.remoteAddress = new InetSocketAddress(host, port);
        return this;
    }

    @Override
    public ChannelFuture bind(ChannelFuture future) {
        this.validate(future);
        if (this.localAddress() == null) {
            throw new IllegalStateException("localAddress not set");
        }
        try {
            this.init(future.channel());
        }
        catch (Throwable t) {
            future.setFailure(t);
            return future;
        }
        if (!Bootstrap.ensureOpen(future)) {
            return future;
        }
        return future.channel().bind(this.localAddress(), future).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }

    public ChannelFuture connect() {
        this.validate();
        Channel channel = this.factory().newChannel();
        return this.connect(channel.newFuture());
    }

    public ChannelFuture connect(ChannelFuture future) {
        this.validate(future);
        if (this.remoteAddress == null) {
            throw new IllegalStateException("remoteAddress not set");
        }
        try {
            this.init(future.channel());
        }
        catch (Throwable t) {
            future.setFailure(t);
            return future;
        }
        if (!Bootstrap.ensureOpen(future)) {
            return future;
        }
        if (this.localAddress() == null) {
            future.channel().connect(this.remoteAddress, future);
        } else {
            future.channel().connect(this.remoteAddress, this.localAddress(), future);
        }
        return future.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }

    private void init(Channel channel) throws Exception {
        if (channel.isActive()) {
            throw new IllegalStateException("channel already active:: " + channel);
        }
        if (channel.isRegistered()) {
            throw new IllegalStateException("channel already registered: " + channel);
        }
        if (!channel.isOpen()) {
            throw new ClosedChannelException();
        }
        ChannelPipeline p = channel.pipeline();
        p.addLast(this.handler());
        for (Map.Entry e2 : this.options().entrySet()) {
            try {
                if (channel.config().setOption(e2.getKey(), e2.getValue())) continue;
                logger.warn("Unknown channel option: " + e2);
            }
            catch (Throwable t) {
                logger.warn("Failed to set a channel option: " + channel, t);
            }
        }
        for (Map.Entry e : this.attrs().entrySet()) {
            channel.attr((AttributeKey)e.getKey()).set(e.getValue());
        }
        this.group().register(channel).syncUninterruptibly();
    }

    @Override
    protected void validate() {
        super.validate();
        if (this.handler() == null) {
            throw new IllegalStateException("handler not set");
        }
    }

    public Bootstrap duplicate() {
        this.validate();
        Bootstrap b = ((Bootstrap)((Bootstrap)((Bootstrap)((Bootstrap)new Bootstrap().group(this.group())).channelFactory(this.factory())).handler(this.handler())).localAddress(this.localAddress())).remoteAddress(this.remoteAddress);
        b.options().putAll(this.options());
        b.attrs().putAll(this.attrs());
        return b;
    }

    @Override
    public Bootstrap channel(Class<? extends Channel> channelClass) {
        if (channelClass == null) {
            throw new NullPointerException("channelClass");
        }
        if (channelClass == AioSocketChannel.class) {
            return (Bootstrap)this.channelFactory(new AioSocketChannelFactory());
        }
        return (Bootstrap)super.channel(channelClass);
    }

    @Override
    public String toString() {
        if (this.remoteAddress == null) {
            return super.toString();
        }
        StringBuilder buf = new StringBuilder(super.toString());
        buf.setLength(buf.length() - 1);
        buf.append(", remoteAddress: ");
        buf.append(this.remoteAddress);
        buf.append(')');
        return buf.toString();
    }

    private final class AioSocketChannelFactory
    implements AbstractBootstrap.ChannelFactory {
        private AioSocketChannelFactory() {
        }

        @Override
        public Channel newChannel() {
            return new AioSocketChannel((AioEventLoopGroup)Bootstrap.this.group());
        }

        public String toString() {
            return AioSocketChannel.class.getSimpleName() + ".class";
        }
    }

}

