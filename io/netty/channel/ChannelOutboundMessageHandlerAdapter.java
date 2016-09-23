
package io.netty.channel;

import io.netty.buffer.ChannelBuf;
import io.netty.buffer.MessageBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelOutboundMessageHandler;

public abstract class ChannelOutboundMessageHandlerAdapter<I>
extends ChannelOutboundHandlerAdapter
implements ChannelOutboundMessageHandler<I> {
    @Override
    public MessageBuf<I> newOutboundBuffer(ChannelHandlerContext ctx) throws Exception {
        return Unpooled.messageBuffer();
    }
}

