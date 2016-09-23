
package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundByteHandlerAdapter;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.EncoderException;

public abstract class ByteToByteEncoder
extends ChannelOutboundByteHandlerAdapter {
    @Override
    public void flush(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
        ByteBuf in = ctx.outboundByteBuffer();
        ByteBuf out = ctx.nextOutboundByteBuffer();
        while (in.readable()) {
            int oldInSize = in.readableBytes();
            try {
                this.encode(ctx, in, out);
            }
            catch (Throwable t) {
                if (t instanceof CodecException) {
                    ctx.fireExceptionCaught(t);
                }
                ctx.fireExceptionCaught(new EncoderException(t));
            }
            if (oldInSize != in.readableBytes()) continue;
            break;
        }
        in.unsafe().discardSomeReadBytes();
        ctx.flush(future);
    }

    public abstract void encode(ChannelHandlerContext var1, ByteBuf var2, ByteBuf var3) throws Exception;
}

