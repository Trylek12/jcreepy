
package io.netty.channel;

import io.netty.buffer.ChannelBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOperationHandlerAdapter;
import io.netty.channel.ChannelOutboundHandler;

public abstract class ChannelOutboundHandlerAdapter
extends ChannelOperationHandlerAdapter
implements ChannelOutboundHandler {
    @Override
    public void freeOutboundBuffer(ChannelHandlerContext ctx, ChannelBuf buf) throws Exception {
        buf.unsafe().free();
    }

    @Override
    public abstract void flush(ChannelHandlerContext var1, ChannelFuture var2) throws Exception;
}

