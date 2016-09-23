
package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ChannelBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandlerUtil;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.ReplayingDecoderBuffer;
import io.netty.util.internal.Signal;

public abstract class ReplayingDecoder<O, S>
extends ByteToMessageDecoder<O> {
    static final Signal REPLAY = new Signal(ReplayingDecoder.class.getName() + ".REPLAY");
    private ByteBuf cumulation;
    private ReplayingDecoderBuffer replayable;
    private S state;
    private int checkpoint = -1;

    protected ReplayingDecoder() {
        this(null);
    }

    protected ReplayingDecoder(S initialState) {
        this.state = initialState;
    }

    protected void checkpoint() {
        this.checkpoint = this.cumulation.readerIndex();
    }

    protected void checkpoint(S state) {
        this.checkpoint();
        this.state(state);
    }

    protected S state() {
        return this.state;
    }

    protected S state(S newState) {
        S oldState = this.state;
        this.state = newState;
        return oldState;
    }

    protected int actualReadableBytes() {
        return this.internalBuffer().readableBytes();
    }

    protected ByteBuf internalBuffer() {
        return this.cumulation;
    }

    @Override
    public ByteBuf newInboundBuffer(ChannelHandlerContext ctx) throws Exception {
        this.cumulation = ctx.alloc().buffer();
        this.replayable = new ReplayingDecoderBuffer(this.cumulation);
        return this.cumulation;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.replayable.terminate();
        ByteBuf in = this.cumulation;
        if (in.readable()) {
            this.callDecode(ctx);
        }
        try {
            if (ChannelHandlerUtil.unfoldAndAdd(ctx, this.decodeLast(ctx, this.replayable), true)) {
                this.fireInboundBufferUpdated(ctx, in);
            }
        }
        catch (Signal replay) {
            replay.expect(REPLAY);
        }
        catch (Throwable t) {
            if (t instanceof CodecException) {
                ctx.fireExceptionCaught(t);
            }
            ctx.fireExceptionCaught(new DecoderException(t));
        }
        ctx.fireChannelInactive();
    }

    @Override
    protected void callDecode(ChannelHandlerContext ctx) {
        ByteBuf in = this.cumulation;
        boolean decoded = false;
        while (in.readable()) {
            try {
                S oldState;
                Object result;
                int oldReaderIndex;
                block10 : {
                    oldReaderIndex = this.checkpoint = in.readerIndex();
                    result = null;
                    oldState = this.state;
                    try {
                        result = this.decode(ctx, this.replayable);
                        if (result == null) {
                            if (oldReaderIndex != in.readerIndex() || oldState != this.state) continue;
                            throw new IllegalStateException("null cannot be returned if no data is consumed and state didn't change.");
                        }
                    }
                    catch (Signal replay) {
                        replay.expect(REPLAY);
                        int checkpoint = this.checkpoint;
                        if (checkpoint < 0) break block10;
                        in.readerIndex(checkpoint);
                    }
                }
                if (result == null) break;
                if (oldReaderIndex == in.readerIndex() && oldState == this.state) {
                    throw new IllegalStateException("decode() method must consume at least one byte if it returned a decoded message (caused by: " + this.getClass() + ')');
                }
                if (!ChannelHandlerUtil.unfoldAndAdd(ctx, result, true)) continue;
                decoded = true;
            }
            catch (Throwable t) {
                if (decoded) {
                    decoded = false;
                    this.fireInboundBufferUpdated(ctx, in);
                }
                if (t instanceof CodecException) {
                    ctx.fireExceptionCaught(t);
                    continue;
                }
                ctx.fireExceptionCaught(new DecoderException(t));
            }
        }
        if (decoded) {
            this.fireInboundBufferUpdated(ctx, in);
        }
    }

    private void fireInboundBufferUpdated(ChannelHandlerContext ctx, ByteBuf in) {
        int oldReaderIndex = in.readerIndex();
        in.unsafe().discardSomeReadBytes();
        int newReaderIndex = in.readerIndex();
        this.checkpoint -= oldReaderIndex - newReaderIndex;
        ctx.fireInboundBufferUpdated();
    }
}

