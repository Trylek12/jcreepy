
package io.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;

public interface ChannelOutboundByteHandler
extends ChannelOutboundHandler {
    @Override
    public ByteBuf newOutboundBuffer(ChannelHandlerContext var1) throws Exception;
}

