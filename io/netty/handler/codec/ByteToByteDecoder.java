
package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundByteHandlerAdapter;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.DecoderException;

public abstract class ByteToByteDecoder
extends ChannelInboundByteHandlerAdapter {
    @Override
    public void inboundBufferUpdated(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        this.callDecode(ctx, in, ctx.nextInboundByteBuffer());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ByteBuf in = ctx.inboundByteBuffer();
        ByteBuf out = ctx.nextInboundByteBuffer();
        if (!in.readable()) {
            this.callDecode(ctx, in, out);
        }
        int oldOutSize = out.readableBytes();
        try {
            this.decodeLast(ctx, in, out);
        }
        catch (Throwable t) {
            if (t instanceof CodecException) {
                ctx.fireExceptionCaught(t);
            }
            ctx.fireExceptionCaught(new DecoderException(t));
        }
        if (out.readableBytes() > oldOutSize) {
            ctx.fireInboundBufferUpdated();
        }
        ctx.fireChannelInactive();
    }

    private void callDecode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) {
        int oldOutSize = out.readableBytes();
        while (in.readable()) {
            int oldInSize = in.readableBytes();
            try {
                this.decode(ctx, in, out);
            }
            catch (Throwable t) {
                if (t instanceof CodecException) {
                    ctx.fireExceptionCaught(t);
                }
                ctx.fireExceptionCaught(new DecoderException(t));
            }
            if (oldInSize != in.readableBytes()) continue;
            break;
        }
        in.unsafe().discardSomeReadBytes();
        if (out.readableBytes() > oldOutSize) {
            ctx.fireInboundBufferUpdated();
        }
    }

    public abstract void decode(ChannelHandlerContext var1, ByteBuf var2, ByteBuf var3) throws Exception;

    public void decodeLast(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
        this.decode(ctx, in, out);
    }
}

