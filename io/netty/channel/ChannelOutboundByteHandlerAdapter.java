
package io.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ChannelBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundByteHandler;
import io.netty.channel.ChannelOutboundHandlerAdapter;

public abstract class ChannelOutboundByteHandlerAdapter
extends ChannelOutboundHandlerAdapter
implements ChannelOutboundByteHandler {
    @Override
    public ByteBuf newOutboundBuffer(ChannelHandlerContext ctx) throws Exception {
        return ctx.alloc().buffer();
    }
}

