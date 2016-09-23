
package io.netty.channel;

import io.netty.buffer.MessageBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;

public interface ChannelOutboundMessageHandler<I>
extends ChannelOutboundHandler {
    @Override
    public MessageBuf<I> newOutboundBuffer(ChannelHandlerContext var1) throws Exception;
}

