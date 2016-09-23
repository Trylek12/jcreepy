
package io.netty.channel.embedded;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ChannelBufType;
import io.netty.buffer.MessageBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.AbstractEmbeddedChannel;
import java.util.Collection;

public class EmbeddedMessageChannel
extends AbstractEmbeddedChannel {
    private static final ChannelMetadata METADATA = new ChannelMetadata(ChannelBufType.MESSAGE, false);

    public /* varargs */ EmbeddedMessageChannel(ChannelHandler ... handlers) {
        super(Unpooled.messageBuffer(), handlers);
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    public MessageBuf<Object> inboundBuffer() {
        return this.pipeline().inboundMessageBuffer();
    }

    public MessageBuf<Object> lastOutboundBuffer() {
        return (MessageBuf)this.lastOutboundBuffer;
    }

    public Object readOutbound() {
        return this.lastOutboundBuffer().poll();
    }

    public boolean writeInbound(Object msg) {
        this.inboundBuffer().add(msg);
        this.pipeline().fireInboundBufferUpdated();
        this.checkException();
        return this.lastInboundByteBuffer().readable() || !this.lastInboundMessageBuffer().isEmpty();
    }

    public boolean writeOutbound(Object msg) {
        this.write(msg);
        this.checkException();
        return !this.lastOutboundBuffer().isEmpty();
    }

    public boolean finish() {
        this.close();
        this.checkException();
        return this.lastInboundByteBuffer().readable() || !this.lastInboundMessageBuffer().isEmpty() || !this.lastOutboundBuffer().isEmpty();
    }

    @Override
    protected void doFlushMessageBuffer(MessageBuf<Object> buf) throws Exception {
        buf.drainTo(this.lastOutboundBuffer());
    }
}

