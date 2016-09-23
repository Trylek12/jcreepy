
package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ChannelBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandlerUtil;
import io.netty.channel.ChannelInboundByteHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventExecutor;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.DecoderException;

public abstract class ByteToMessageDecoder<O>
extends ChannelInboundHandlerAdapter
implements ChannelInboundByteHandler {
    private ChannelHandlerContext ctx;

    @Override
    public void beforeAdd(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        super.beforeAdd(ctx);
    }

    @Override
    public ByteBuf newInboundBuffer(ChannelHandlerContext ctx) throws Exception {
        return ctx.alloc().buffer();
    }

    @Override
    public void inboundBufferUpdated(ChannelHandlerContext ctx) throws Exception {
        this.callDecode(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ByteBuf in = ctx.inboundByteBuffer();
        if (in.readable()) {
            this.callDecode(ctx);
        }
        try {
            if (ChannelHandlerUtil.unfoldAndAdd(ctx, this.decodeLast(ctx, in), true)) {
                ctx.fireInboundBufferUpdated();
            }
        }
        catch (Throwable t) {
            if (t instanceof CodecException) {
                ctx.fireExceptionCaught(t);
            }
            ctx.fireExceptionCaught(new DecoderException(t));
        }
        ctx.fireChannelInactive();
    }

    protected void callDecode(ChannelHandlerContext ctx) {
        ByteBuf in = ctx.inboundByteBuffer();
        boolean decoded = false;
        while (in.readable()) {
            try {
                int oldInputLength = in.readableBytes();
                O o = this.decode(ctx, in);
                if (o == null) {
                    if (oldInputLength != in.readableBytes()) continue;
                    break;
                }
                if (oldInputLength == in.readableBytes()) {
                    throw new IllegalStateException("decode() did not read anything but decoded a message.");
                }
                if (!ChannelHandlerUtil.unfoldAndAdd(ctx, o, true)) break;
                decoded = true;
            }
            catch (Throwable t) {
                in.unsafe().discardSomeReadBytes();
                if (decoded) {
                    decoded = false;
                    ctx.fireInboundBufferUpdated();
                }
                if (t instanceof CodecException) {
                    ctx.fireExceptionCaught(t);
                    continue;
                }
                ctx.fireExceptionCaught(new DecoderException(t));
            }
        }
        in.unsafe().discardSomeReadBytes();
        if (decoded) {
            ctx.fireInboundBufferUpdated();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void replace(String newHandlerName, ChannelInboundByteHandler newHandler) {
        if (!this.ctx.executor().inEventLoop()) {
            throw new IllegalStateException("not in event loop");
        }
        this.ctx.pipeline().addAfter(this.ctx.name(), newHandlerName, newHandler);
        ByteBuf in = this.ctx.inboundByteBuffer();
        try {
            if (in.readable()) {
                this.ctx.nextInboundByteBuffer().writeBytes(in);
                this.ctx.fireInboundBufferUpdated();
            }
        }
        finally {
            this.ctx.pipeline().remove(this);
        }
    }

    public abstract O decode(ChannelHandlerContext var1, ByteBuf var2) throws Exception;

    public O decodeLast(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        return this.decode(ctx, in);
    }
}

