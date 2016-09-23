
package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ChannelBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundByteHandler;
import io.netty.channel.ChannelOutboundByteHandler;
import io.netty.handler.codec.ByteToByteDecoder;
import io.netty.handler.codec.ByteToByteEncoder;

public abstract class ByteToByteCodec
extends ChannelHandlerAdapter
implements ChannelInboundByteHandler,
ChannelOutboundByteHandler {
    private final ByteToByteEncoder encoder;
    private final ByteToByteDecoder decoder;

    public ByteToByteCodec() {
        this.encoder = new ByteToByteEncoder(){

            @Override
            public void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
                ByteToByteCodec.this.encode(ctx, in, out);
            }
        };
        this.decoder = new ByteToByteDecoder(){

            @Override
            public void decode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
                ByteToByteCodec.this.decode(ctx, in, out);
            }
        };
    }

    @Override
    public ByteBuf newInboundBuffer(ChannelHandlerContext ctx) throws Exception {
        return this.decoder.newInboundBuffer(ctx);
    }

    @Override
    public void inboundBufferUpdated(ChannelHandlerContext ctx) throws Exception {
        this.decoder.inboundBufferUpdated(ctx);
    }

    @Override
    public ByteBuf newOutboundBuffer(ChannelHandlerContext ctx) throws Exception {
        return this.encoder.newOutboundBuffer(ctx);
    }

    @Override
    public void flush(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
        this.encoder.flush(ctx, future);
    }

    @Override
    public void freeInboundBuffer(ChannelHandlerContext ctx, ChannelBuf buf) throws Exception {
        this.decoder.freeInboundBuffer(ctx, buf);
    }

    @Override
    public void freeOutboundBuffer(ChannelHandlerContext ctx, ChannelBuf buf) throws Exception {
        this.encoder.freeOutboundBuffer(ctx, buf);
    }

    public abstract void encode(ChannelHandlerContext var1, ByteBuf var2, ByteBuf var3) throws Exception;

    public abstract void decode(ChannelHandlerContext var1, ByteBuf var2, ByteBuf var3) throws Exception;

}

