
package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.TooLongFrameException;

public class LengthFieldBasedFrameDecoder
extends ByteToMessageDecoder<Object> {
    private final int maxFrameLength;
    private final int lengthFieldOffset;
    private final int lengthFieldLength;
    private final int lengthFieldEndOffset;
    private final int lengthAdjustment;
    private final int initialBytesToStrip;
    private final boolean failFast;
    private boolean discardingTooLongFrame;
    private long tooLongFrameLength;
    private long bytesToDiscard;

    public LengthFieldBasedFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength) {
        this(maxFrameLength, lengthFieldOffset, lengthFieldLength, 0, 0);
    }

    public LengthFieldBasedFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        this(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip, true);
    }

    public LengthFieldBasedFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip, boolean failFast) {
        if (maxFrameLength <= 0) {
            throw new IllegalArgumentException("maxFrameLength must be a positive integer: " + maxFrameLength);
        }
        if (lengthFieldOffset < 0) {
            throw new IllegalArgumentException("lengthFieldOffset must be a non-negative integer: " + lengthFieldOffset);
        }
        if (initialBytesToStrip < 0) {
            throw new IllegalArgumentException("initialBytesToStrip must be a non-negative integer: " + initialBytesToStrip);
        }
        if (lengthFieldLength != 1 && lengthFieldLength != 2 && lengthFieldLength != 3 && lengthFieldLength != 4 && lengthFieldLength != 8) {
            throw new IllegalArgumentException("lengthFieldLength must be either 1, 2, 3, 4, or 8: " + lengthFieldLength);
        }
        if (lengthFieldOffset > maxFrameLength - lengthFieldLength) {
            throw new IllegalArgumentException("maxFrameLength (" + maxFrameLength + ") " + "must be equal to or greater than " + "lengthFieldOffset (" + lengthFieldOffset + ") + " + "lengthFieldLength (" + lengthFieldLength + ").");
        }
        this.maxFrameLength = maxFrameLength;
        this.lengthFieldOffset = lengthFieldOffset;
        this.lengthFieldLength = lengthFieldLength;
        this.lengthAdjustment = lengthAdjustment;
        this.lengthFieldEndOffset = lengthFieldOffset + lengthFieldLength;
        this.initialBytesToStrip = initialBytesToStrip;
        this.failFast = failFast;
    }

    @Override
    public Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        long frameLength;
        if (this.discardingTooLongFrame) {
            long bytesToDiscard = this.bytesToDiscard;
            int localBytesToDiscard = (int)Math.min(bytesToDiscard, (long)in.readableBytes());
            in.skipBytes(localBytesToDiscard);
            this.bytesToDiscard = bytesToDiscard -= (long)localBytesToDiscard;
            this.failIfNecessary(ctx, false);
            return null;
        }
        if (in.readableBytes() < this.lengthFieldEndOffset) {
            return null;
        }
        int actualLengthFieldOffset = in.readerIndex() + this.lengthFieldOffset;
        switch (this.lengthFieldLength) {
            case 1: {
                frameLength = in.getUnsignedByte(actualLengthFieldOffset);
                break;
            }
            case 2: {
                frameLength = in.getUnsignedShort(actualLengthFieldOffset);
                break;
            }
            case 3: {
                frameLength = in.getUnsignedMedium(actualLengthFieldOffset);
                break;
            }
            case 4: {
                frameLength = in.getUnsignedInt(actualLengthFieldOffset);
                break;
            }
            case 8: {
                frameLength = in.getLong(actualLengthFieldOffset);
                break;
            }
            default: {
                throw new Error("should not reach here");
            }
        }
        if (frameLength < 0) {
            in.skipBytes(this.lengthFieldEndOffset);
            throw new CorruptedFrameException("negative pre-adjustment length field: " + frameLength);
        }
        if ((frameLength += (long)(this.lengthAdjustment + this.lengthFieldEndOffset)) < (long)this.lengthFieldEndOffset) {
            in.skipBytes(this.lengthFieldEndOffset);
            throw new CorruptedFrameException("Adjusted frame length (" + frameLength + ") is less " + "than lengthFieldEndOffset: " + this.lengthFieldEndOffset);
        }
        if (frameLength > (long)this.maxFrameLength) {
            this.discardingTooLongFrame = true;
            this.tooLongFrameLength = frameLength;
            this.bytesToDiscard = frameLength - (long)in.readableBytes();
            in.skipBytes(in.readableBytes());
            this.failIfNecessary(ctx, true);
            return null;
        }
        int frameLengthInt = (int)frameLength;
        if (in.readableBytes() < frameLengthInt) {
            return null;
        }
        if (this.initialBytesToStrip > frameLengthInt) {
            in.skipBytes(frameLengthInt);
            throw new CorruptedFrameException("Adjusted frame length (" + frameLength + ") is less " + "than initialBytesToStrip: " + this.initialBytesToStrip);
        }
        in.skipBytes(this.initialBytesToStrip);
        int readerIndex = in.readerIndex();
        int actualFrameLength = frameLengthInt - this.initialBytesToStrip;
        ByteBuf frame = this.extractFrame(in, readerIndex, actualFrameLength);
        in.readerIndex(readerIndex + actualFrameLength);
        return frame;
    }

    private void failIfNecessary(ChannelHandlerContext ctx, boolean firstDetectionOfTooLongFrame) {
        if (this.bytesToDiscard == 0) {
            long tooLongFrameLength = this.tooLongFrameLength;
            this.tooLongFrameLength = 0;
            this.discardingTooLongFrame = false;
            if (!this.failFast || this.failFast && firstDetectionOfTooLongFrame) {
                this.fail(ctx, tooLongFrameLength);
            }
        } else if (this.failFast && firstDetectionOfTooLongFrame) {
            this.fail(ctx, this.tooLongFrameLength);
        }
    }

    protected ByteBuf extractFrame(ByteBuf buffer, int index, int length) {
        ByteBuf frame = Unpooled.buffer(length);
        frame.writeBytes(buffer, index, length);
        return frame;
    }

    private void fail(ChannelHandlerContext ctx, long frameLength) {
        if (frameLength > 0) {
            ctx.fireExceptionCaught(new TooLongFrameException("Adjusted frame length exceeds " + this.maxFrameLength + ": " + frameLength + " - discarded"));
        } else {
            ctx.fireExceptionCaught(new TooLongFrameException("Adjusted frame length exceeds " + this.maxFrameLength + " - discarding"));
        }
    }
}

