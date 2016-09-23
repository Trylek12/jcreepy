
package io.netty.handler.codec;

import io.netty.buffer.ChannelBuf;
import io.netty.buffer.MessageBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandlerUtil;
import io.netty.channel.ChannelInboundMessageHandler;
import io.netty.channel.ChannelOutboundMessageHandler;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;

public abstract class MessageToMessageCodec<INBOUND_IN, INBOUND_OUT, OUTBOUND_IN, OUTBOUND_OUT>
extends ChannelHandlerAdapter
implements ChannelInboundMessageHandler<INBOUND_IN>,
ChannelOutboundMessageHandler<OUTBOUND_IN> {
    private final MessageToMessageEncoder<OUTBOUND_IN, OUTBOUND_OUT> encoder;
    private final MessageToMessageDecoder<INBOUND_IN, INBOUND_OUT> decoder;
    private final Class<?>[] acceptedInboundMsgTypes;
    private final Class<?>[] acceptedOutboundMsgTypes;

    protected MessageToMessageCodec() {
        this(null, null);
    }

    protected MessageToMessageCodec(Class<?>[] acceptedInboundMsgTypes, Class<?>[] acceptedOutboundMsgTypes) {
        this.encoder = new MessageToMessageEncoder<OUTBOUND_IN, OUTBOUND_OUT>(new Class[0]){

            @Override
            public boolean isEncodable(Object msg) throws Exception {
                return MessageToMessageCodec.this.isEncodable(msg);
            }

            @Override
            public OUTBOUND_OUT encode(ChannelHandlerContext ctx, OUTBOUND_IN msg) throws Exception {
                return MessageToMessageCodec.this.encode(ctx, msg);
            }
        };
        this.decoder = new MessageToMessageDecoder<INBOUND_IN, INBOUND_OUT>(new Class[0]){

            @Override
            public boolean isDecodable(Object msg) throws Exception {
                return MessageToMessageCodec.this.isDecodable(msg);
            }

            @Override
            public INBOUND_OUT decode(ChannelHandlerContext ctx, INBOUND_IN msg) throws Exception {
                return MessageToMessageCodec.this.decode(ctx, msg);
            }
        };
        this.acceptedInboundMsgTypes = ChannelHandlerUtil.acceptedMessageTypes(acceptedInboundMsgTypes);
        this.acceptedOutboundMsgTypes = ChannelHandlerUtil.acceptedMessageTypes(acceptedOutboundMsgTypes);
    }

    @Override
    public MessageBuf<INBOUND_IN> newInboundBuffer(ChannelHandlerContext ctx) throws Exception {
        return this.decoder.newInboundBuffer(ctx);
    }

    @Override
    public void freeInboundBuffer(ChannelHandlerContext ctx, ChannelBuf buf) throws Exception {
        buf.unsafe().free();
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
    public void freeOutboundBuffer(ChannelHandlerContext ctx, ChannelBuf buf) throws Exception {
        buf.unsafe().free();
    }

    @Override
    public void flush(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
        this.encoder.flush(ctx, future);
    }

    public boolean isDecodable(Object msg) throws Exception {
        return ChannelHandlerUtil.acceptMessage(this.acceptedInboundMsgTypes, msg);
    }

    public boolean isEncodable(Object msg) throws Exception {
        return ChannelHandlerUtil.acceptMessage(this.acceptedOutboundMsgTypes, msg);
    }

    public abstract OUTBOUND_OUT encode(ChannelHandlerContext var1, OUTBOUND_IN var2) throws Exception;

    public abstract INBOUND_OUT decode(ChannelHandlerContext var1, INBOUND_IN var2) throws Exception;

}

