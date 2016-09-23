
package io.netty.channel;

import io.netty.buffer.ChannelBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOperationHandler;

public interface ChannelOutboundHandler
extends ChannelOperationHandler {
    public ChannelBuf newOutboundBuffer(ChannelHandlerContext var1) throws Exception;

    public void freeOutboundBuffer(ChannelHandlerContext var1, ChannelBuf var2) throws Exception;
}

