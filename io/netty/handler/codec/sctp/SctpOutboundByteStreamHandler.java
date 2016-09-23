
package io.netty.handler.codec.sctp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.MessageBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundByteHandlerAdapter;
import io.netty.channel.socket.SctpMessage;
import io.netty.handler.codec.EncoderException;

public class SctpOutboundByteStreamHandler
extends ChannelOutboundByteHandlerAdapter {
    private final int streamIdentifier;
    private final int protocolIdentifier;

    public SctpOutboundByteStreamHandler(int streamIdentifier, int protocolIdentifier) {
        this.streamIdentifier = streamIdentifier;
        this.protocolIdentifier = protocolIdentifier;
    }

    @Override
    public void flush(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
        ByteBuf in = ctx.outboundByteBuffer();
        try {
            MessageBuf<Object> out = ctx.nextOutboundMessageBuffer();
            ByteBuf payload = Unpooled.buffer(in.readableBytes());
            payload.writeBytes(in);
            out.add(new SctpMessage(this.streamIdentifier, this.protocolIdentifier, payload));
            in.discardReadBytes();
        }
        catch (Throwable t) {
            ctx.fireExceptionCaught(new EncoderException(t));
        }
        ctx.flush(future);
    }
}

