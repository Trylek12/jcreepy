
package io.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.MessageBuf;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundInvoker;
import io.netty.channel.ChannelPropertyAccess;
import io.netty.channel.EventLoop;
import io.netty.channel.FileRegion;
import io.netty.util.AttributeMap;
import java.net.SocketAddress;

public interface Channel
extends AttributeMap,
ChannelOutboundInvoker,
ChannelPropertyAccess,
Comparable<Channel> {
    public Integer id();

    public EventLoop eventLoop();

    public Channel parent();

    public ChannelConfig config();

    public boolean isOpen();

    public boolean isRegistered();

    public boolean isActive();

    public ChannelMetadata metadata();

    public ByteBuf outboundByteBuffer();

    public <T> MessageBuf<T> outboundMessageBuffer();

    public SocketAddress localAddress();

    public SocketAddress remoteAddress();

    public ChannelFuture closeFuture();

    public Unsafe unsafe();

    public static interface Unsafe {
        public ChannelHandlerContext directOutboundContext();

        public ChannelFuture voidFuture();

        public SocketAddress localAddress();

        public SocketAddress remoteAddress();

        public void register(EventLoop var1, ChannelFuture var2);

        public void bind(SocketAddress var1, ChannelFuture var2);

        public void connect(SocketAddress var1, SocketAddress var2, ChannelFuture var3);

        public void disconnect(ChannelFuture var1);

        public void close(ChannelFuture var1);

        public void closeForcibly();

        public void deregister(ChannelFuture var1);

        public void flush(ChannelFuture var1);

        public void flushNow();

        public void suspendRead();

        public void resumeRead();

        public void sendFile(FileRegion var1, ChannelFuture var2);
    }

}

