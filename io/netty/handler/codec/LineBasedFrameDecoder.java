
package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.TooLongFrameException;

public class LineBasedFrameDecoder
extends ByteToMessageDecoder<ByteBuf> {
    private final int maxLength;
    private final boolean failFast;
    private final boolean stripDelimiter;
    private boolean discarding;

    public LineBasedFrameDecoder(int maxLength) {
        this(maxLength, true, false);
    }

    public LineBasedFrameDecoder(int maxLength, boolean stripDelimiter, boolean failFast) {
        this.maxLength = maxLength;
        this.failFast = failFast;
        this.stripDelimiter = stripDelimiter;
    }

    @Override
    public ByteBuf decode(ChannelHandlerContext ctx, ByteBuf buffer) throws Exception {
        int eol = LineBasedFrameDecoder.findEndOfLine(buffer);
        if (eol != -1) {
            ByteBuf frame;
            int length = eol - buffer.readerIndex();
            assert (length >= 0);
            if (this.discarding) {
                frame = null;
                buffer.skipBytes(length);
                if (!this.failFast) {
                    this.fail(ctx, "over " + (this.maxLength + length) + " bytes");
                }
            } else {
                byte delim = buffer.getByte(buffer.readerIndex() + length);
                int delimLength = delim == 13 ? 2 : 1;
                if (this.stripDelimiter) {
                    frame = buffer.readBytes(length);
                    buffer.skipBytes(delimLength);
                } else {
                    frame = buffer.readBytes(length + delimLength);
                }
            }
            return frame;
        }
        int buffered = buffer.readableBytes();
        if (!this.discarding && buffered > this.maxLength) {
            this.discarding = true;
            if (this.failFast) {
                this.fail(ctx, "" + buffered + " bytes buffered already");
            }
        }
        if (this.discarding) {
            buffer.skipBytes(buffer.readableBytes());
        }
        return null;
    }

    private void fail(ChannelHandlerContext ctx, String msg) {
        ctx.fireExceptionCaught(new TooLongFrameException("Frame length exceeds " + this.maxLength + " (" + msg + ')'));
    }

    private static int findEndOfLine(ByteBuf buffer) {
        int n = buffer.writerIndex();
        for (int i = buffer.readerIndex(); i < n; ++i) {
            byte b = buffer.getByte(i);
            if (b == 10) {
                return i;
            }
            if (b != 13 || i >= n - 1 || buffer.getByte(i + 1) != 10) continue;
            return i;
        }
        return -1;
    }
}

