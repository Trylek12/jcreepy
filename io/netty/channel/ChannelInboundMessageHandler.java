
package io.netty.channel;

import io.netty.buffer.MessageBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;

public interface ChannelInboundMessageHandler<I>
extends ChannelInboundHandler {
    @Override
    public MessageBuf<I> newInboundBuffer(ChannelHandlerContext var1) throws Exception;
}

