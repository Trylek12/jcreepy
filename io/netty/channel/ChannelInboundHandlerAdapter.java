
package io.netty.channel;

import io.netty.buffer.ChannelBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelStateHandlerAdapter;

public abstract class ChannelInboundHandlerAdapter
extends ChannelStateHandlerAdapter
implements ChannelInboundHandler {
    @Override
    public void freeInboundBuffer(ChannelHandlerContext ctx, ChannelBuf buf) throws Exception {
        buf.unsafe().free();
    }

    @Override
    public abstract void inboundBufferUpdated(ChannelHandlerContext var1) throws Exception;
}

