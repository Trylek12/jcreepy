
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

public class EmbeddedByteChannel
extends AbstractEmbeddedChannel {
    private static final ChannelMetadata METADATA = new ChannelMetadata(ChannelBufType.BYTE, false);

    public /* varargs */ EmbeddedByteChannel(ChannelHandler ... handlers) {
        super(Unpooled.buffer(), handlers);
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    public ByteBuf inboundBuffer() {
        return this.pipeline().inboundByteBuffer();
    }

    public ByteBuf lastOutboundBuffer() {
        return (ByteBuf)this.lastOutboundBuffer;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public ByteBuf readOutbound() {
        if (!this.lastOutboundBuffer().readable()) {
            return null;
        }
        try {
            ByteBuf byteBuf = this.lastOutboundBuffer().readBytes(this.lastOutboundBuffer().readableBytes());
            return byteBuf;
        }
        finally {
            this.lastOutboundBuffer().clear();
        }
    }

    public boolean writeInbound(ByteBuf data) {
        this.inboundBuffer().writeBytes(data);
        this.pipeline().fireInboundBufferUpdated();
        this.checkException();
        return this.lastInboundByteBuffer().readable() || !this.lastInboundMessageBuffer().isEmpty();
    }

    public boolean writeOutbound(Object msg) {
        this.write(msg);
        this.checkException();
        return this.lastOutboundBuffer().readable();
    }

    public boolean finish() {
        this.close();
        this.checkException();
        return this.lastInboundByteBuffer().readable() || !this.lastInboundMessageBuffer().isEmpty() || this.lastOutboundBuffer().readable();
    }

    @Override
    protected void doFlushByteBuffer(ByteBuf buf) throws Exception {
        if (!this.lastOutboundBuffer().readable()) {
            this.lastOutboundBuffer().discardReadBytes();
        }
        this.lastOutboundBuffer().writeBytes(buf);
    }
}

