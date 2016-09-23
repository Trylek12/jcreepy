
package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ChannelBuf;
import io.netty.buffer.MessageBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundByteHandler;
import io.netty.channel.ChannelOutboundMessageHandler;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

public abstract class ByteToMessageCodec<INBOUND_OUT, OUTBOUND_IN>
extends ChannelHandlerAdapter
implements ChannelInboundByteHandler,
ChannelOutboundMessageHandler<OUTBOUND_IN> {
    private final MessageToByteEncoder<OUTBOUND_IN> encoder;
    private final ByteToMessageDecoder<INBOUND_OUT> decoder;

    public ByteToMessageCodec() {
        this.encoder = new MessageToByteEncoder<OUTBOUND_IN>(new Class[0]){

            @Override
            public void encode(ChannelHandlerContext ctx, OUTBOUND_IN msg, ByteBuf out) throws Exception {
                ByteToMessageCodec.this.encode(ctx, msg, out);
            }
        };
        this.decoder = new ByteToMessageDecoder<INBOUND_OUT>(){

            @Override
            public INBOUND_OUT decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
                return ByteToMessageCodec.this.decode(ctx, in);
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
    public MessageBuf<OUTBOUND_IN> newOutboundBuffer(ChannelHandlerContext ctx) throws Exception {
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

    public abstract void encode(ChannelHandlerContext var1, OUTBOUND_IN var2, ByteBuf var3) throws Exception;

    public abstract INBOUND_OUT decode(ChannelHandlerContext var1, ByteBuf var2) throws Exception;

}

