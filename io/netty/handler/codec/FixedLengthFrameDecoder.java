
package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ChannelBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class FixedLengthFrameDecoder
extends ByteToMessageDecoder<Object> {
    private final int frameLength;
    private final boolean allocateFullBuffer;

    public FixedLengthFrameDecoder(int frameLength) {
        this(frameLength, false);
    }

    public FixedLengthFrameDecoder(int frameLength, boolean allocateFullBuffer) {
        if (frameLength <= 0) {
            throw new IllegalArgumentException("frameLength must be a positive integer: " + frameLength);
        }
        this.frameLength = frameLength;
        this.allocateFullBuffer = allocateFullBuffer;
    }

    @Override
    public ByteBuf newInboundBuffer(ChannelHandlerContext ctx) throws Exception {
        if (this.allocateFullBuffer) {
            return ctx.alloc().buffer(this.frameLength);
        }
        return super.newInboundBuffer(ctx);
    }

    @Override
    public Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        if (in.readableBytes() < this.frameLength) {
            return null;
        }
        return in.readBytes(this.frameLength);
    }
}

