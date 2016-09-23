
package io.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.MessageBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerType;
import io.netty.channel.ChannelInboundInvoker;
import io.netty.channel.ChannelOutboundInvoker;
import io.netty.channel.ChannelPropertyAccess;
import io.netty.channel.EventExecutor;
import io.netty.util.AttributeMap;
import java.util.Set;

public interface ChannelHandlerContext
extends AttributeMap,
ChannelPropertyAccess,
ChannelInboundInvoker,
ChannelOutboundInvoker {
    public Channel channel();

    public EventExecutor executor();

    public String name();

    public ChannelHandler handler();

    public Set<ChannelHandlerType> types();

    public boolean hasInboundByteBuffer();

    public boolean hasInboundMessageBuffer();

    public ByteBuf inboundByteBuffer();

    public <T> MessageBuf<T> inboundMessageBuffer();

    public boolean hasOutboundByteBuffer();

    public boolean hasOutboundMessageBuffer();

    public ByteBuf outboundByteBuffer();

    public <T> MessageBuf<T> outboundMessageBuffer();

    public ByteBuf replaceInboundByteBuffer(ByteBuf var1);

    public <T> MessageBuf<T> replaceInboundMessageBuffer(MessageBuf<T> var1);

    public ByteBuf replaceOutboundByteBuffer(ByteBuf var1);

    public <T> MessageBuf<T> replaceOutboundMessageBuffer(MessageBuf<T> var1);

    public boolean hasNextInboundByteBuffer();

    public boolean hasNextInboundMessageBuffer();

    public ByteBuf nextInboundByteBuffer();

    public MessageBuf<Object> nextInboundMessageBuffer();

    public boolean hasNextOutboundByteBuffer();

    public boolean hasNextOutboundMessageBuffer();

    public ByteBuf nextOutboundByteBuffer();

    public MessageBuf<Object> nextOutboundMessageBuffer();

    public boolean isReadable();

    public void readable(boolean var1);
}

