
package io.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;

public interface ChannelInboundByteHandler
extends ChannelInboundHandler {
    @Override
    public ByteBuf newInboundBuffer(ChannelHandlerContext var1) throws Exception;
}

