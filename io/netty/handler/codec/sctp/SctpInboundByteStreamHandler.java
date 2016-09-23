
package io.netty.handler.codec.sctp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.MessageBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.channel.socket.SctpMessage;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.sctp.SctpMessageCompletionHandler;

public class SctpInboundByteStreamHandler
extends ChannelInboundMessageHandlerAdapter<SctpMessage> {
    private final int protocolIdentifier;
    private final int streamIdentifier;

    public SctpInboundByteStreamHandler(int protocolIdentifier, int streamIdentifier) {
        super(new Class[0]);
        this.protocolIdentifier = protocolIdentifier;
        this.streamIdentifier = streamIdentifier;
    }

    protected boolean isDecodable(SctpMessage msg) {
        return msg.getProtocolIdentifier() == this.protocolIdentifier && msg.getStreamIdentifier() == this.streamIdentifier;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, SctpMessage msg) throws Exception {
        if (!this.isDecodable(msg)) {
            ctx.nextInboundMessageBuffer().add(msg);
            ctx.fireInboundBufferUpdated();
            return;
        }
        if (!msg.isComplete()) {
            throw new CodecException(String.format("Received SctpMessage is not complete, please add %s in the pipeline before this handler", SctpMessageCompletionHandler.class.getSimpleName()));
        }
        ctx.nextInboundByteBuffer().writeBytes(msg.getPayloadBuffer());
        ctx.fireInboundBufferUpdated();
    }
}

