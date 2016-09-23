
package io.netty.handler.codec.sctp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.MessageBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.channel.socket.SctpMessage;
import java.util.HashMap;
import java.util.Map;

public class SctpMessageCompletionHandler
extends ChannelInboundMessageHandlerAdapter<SctpMessage> {
    private final Map<Integer, ByteBuf> fragments = new HashMap<Integer, ByteBuf>();

    public SctpMessageCompletionHandler() {
        super(new Class[0]);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, SctpMessage msg) throws Exception {
        ByteBuf byteBuf = msg.getPayloadBuffer();
        int protocolIdentifier = msg.getProtocolIdentifier();
        int streamIdentifier = msg.getStreamIdentifier();
        boolean isComplete = msg.isComplete();
        ByteBuf frag = this.fragments.containsKey(streamIdentifier) ? this.fragments.remove(streamIdentifier) : Unpooled.EMPTY_BUFFER;
        if (isComplete && !frag.readable()) {
            this.fireAssembledMessage(ctx, msg);
        } else if (!isComplete && frag.readable()) {
            this.fragments.put(streamIdentifier, Unpooled.wrappedBuffer(frag, byteBuf));
        } else if (isComplete && frag.readable()) {
            this.fragments.remove(streamIdentifier);
            SctpMessage assembledMsg = new SctpMessage(protocolIdentifier, streamIdentifier, Unpooled.wrappedBuffer(frag, byteBuf));
            this.fireAssembledMessage(ctx, assembledMsg);
        } else {
            this.fragments.put(streamIdentifier, byteBuf);
        }
    }

    protected void fireAssembledMessage(ChannelHandlerContext ctx, SctpMessage assembledMsg) {
        ctx.nextInboundMessageBuffer().add(assembledMsg);
        ctx.fireInboundBufferUpdated();
    }
}

