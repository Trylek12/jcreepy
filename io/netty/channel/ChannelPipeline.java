
package io.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.MessageBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundInvoker;
import io.netty.channel.ChannelOutboundInvoker;
import io.netty.channel.EventExecutorGroup;
import java.util.List;
import java.util.Map;

public interface ChannelPipeline
extends ChannelInboundInvoker,
ChannelOutboundInvoker {
    public <T> MessageBuf<T> inboundMessageBuffer();

    public ByteBuf inboundByteBuffer();

    public <T> MessageBuf<T> outboundMessageBuffer();

    public ByteBuf outboundByteBuffer();

    public ChannelPipeline addFirst(String var1, ChannelHandler var2);

    public ChannelPipeline addFirst(EventExecutorGroup var1, String var2, ChannelHandler var3);

    public ChannelPipeline addLast(String var1, ChannelHandler var2);

    public ChannelPipeline addLast(EventExecutorGroup var1, String var2, ChannelHandler var3);

    public ChannelPipeline addBefore(String var1, String var2, ChannelHandler var3);

    public ChannelPipeline addBefore(EventExecutorGroup var1, String var2, String var3, ChannelHandler var4);

    public ChannelPipeline addAfter(String var1, String var2, ChannelHandler var3);

    public ChannelPipeline addAfter(EventExecutorGroup var1, String var2, String var3, ChannelHandler var4);

    public /* varargs */ ChannelPipeline addFirst(ChannelHandler ... var1);

    public /* varargs */ ChannelPipeline addFirst(EventExecutorGroup var1, ChannelHandler ... var2);

    public /* varargs */ ChannelPipeline addLast(ChannelHandler ... var1);

    public /* varargs */ ChannelPipeline addLast(EventExecutorGroup var1, ChannelHandler ... var2);

    public void remove(ChannelHandler var1);

    public ChannelHandler remove(String var1);

    public <T extends ChannelHandler> T remove(Class<T> var1);

    public ChannelHandler removeFirst();

    public ChannelHandler removeLast();

    public void replace(ChannelHandler var1, String var2, ChannelHandler var3);

    public ChannelHandler replace(String var1, String var2, ChannelHandler var3);

    public <T extends ChannelHandler> T replace(Class<T> var1, String var2, ChannelHandler var3);

    public ChannelHandler first();

    public ChannelHandlerContext firstContext();

    public ChannelHandler last();

    public ChannelHandlerContext lastContext();

    public ChannelHandler get(String var1);

    public <T extends ChannelHandler> T get(Class<T> var1);

    public ChannelHandlerContext context(ChannelHandler var1);

    public ChannelHandlerContext context(String var1);

    public ChannelHandlerContext context(Class<? extends ChannelHandler> var1);

    public Channel channel();

    public List<String> names();

    public Map<String, ChannelHandler> toMap();
}

