
package io.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ChannelBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundByteHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;

public abstract class ChannelInboundByteHandlerAdapter
extends ChannelInboundHandlerAdapter
implements ChannelInboundByteHandler {
    @Override
    public ByteBuf newInboundBuffer(ChannelHandlerContext ctx) throws Exception {
        return ctx.alloc().buffer();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public final void inboundBufferUpdated(ChannelHandlerContext ctx) throws Exception {
        ByteBuf in = ctx.inboundByteBuffer();
        try {
            this.inboundBufferUpdated(ctx, in);
        }
        finally {
            if (!in.readable()) {
                in.unsafe().discardSomeReadBytes();
            }
        }
    }

    public abstract void inboundBufferUpdated(ChannelHandlerContext var1, ByteBuf var2) throws Exception;
}

