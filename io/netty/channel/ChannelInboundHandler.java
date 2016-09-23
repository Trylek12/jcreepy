
package io.netty.channel;

import io.netty.buffer.ChannelBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelStateHandler;

public interface ChannelInboundHandler
extends ChannelStateHandler {
    public ChannelBuf newInboundBuffer(ChannelHandlerContext var1) throws Exception;

    public void freeInboundBuffer(ChannelHandlerContext var1, ChannelBuf var2) throws Exception;
}

