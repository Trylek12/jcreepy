
package io.netty.handler.codec.compression;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventExecutor;
import io.netty.handler.codec.compression.ZlibEncoder;
import io.netty.handler.codec.compression.ZlibUtil;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.util.internal.jzlib.JZlib;
import io.netty.util.internal.jzlib.ZStream;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class JZlibEncoder
extends ZlibEncoder {
    private static final byte[] EMPTY_ARRAY = new byte[0];
    private final ZStream z = new ZStream();
    private final AtomicBoolean finished = new AtomicBoolean();
    private volatile ChannelHandlerContext ctx;

    public JZlibEncoder() {
        this(6);
    }

    public JZlibEncoder(int compressionLevel) {
        this(ZlibWrapper.ZLIB, compressionLevel);
    }

    public JZlibEncoder(ZlibWrapper wrapper) {
        this(wrapper, 6);
    }

    public JZlibEncoder(ZlibWrapper wrapper, int compressionLevel) {
        this(wrapper, compressionLevel, 15, 8);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public JZlibEncoder(ZlibWrapper wrapper, int compressionLevel, int windowBits, int memLevel) {
        if (compressionLevel < 0 || compressionLevel > 9) {
            throw new IllegalArgumentException("compressionLevel: " + compressionLevel + " (expected: 0-9)");
        }
        if (windowBits < 9 || windowBits > 15) {
            throw new IllegalArgumentException("windowBits: " + windowBits + " (expected: 9-15)");
        }
        if (memLevel < 1 || memLevel > 9) {
            throw new IllegalArgumentException("memLevel: " + memLevel + " (expected: 1-9)");
        }
        if (wrapper == null) {
            throw new NullPointerException("wrapper");
        }
        if (wrapper == ZlibWrapper.ZLIB_OR_NONE) {
            throw new IllegalArgumentException("wrapper '" + (Object)((Object)ZlibWrapper.ZLIB_OR_NONE) + "' is not " + "allowed for compression.");
        }
        ZStream zStream = this.z;
        synchronized (zStream) {
            int resultCode = this.z.deflateInit(compressionLevel, windowBits, memLevel, ZlibUtil.convertWrapperType(wrapper));
            if (resultCode != 0) {
                ZlibUtil.fail(this.z, "initialization failure", resultCode);
            }
        }
    }

    public JZlibEncoder(byte[] dictionary) {
        this(6, dictionary);
    }

    public JZlibEncoder(int compressionLevel, byte[] dictionary) {
        this(compressionLevel, 15, 8, dictionary);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public JZlibEncoder(int compressionLevel, int windowBits, int memLevel, byte[] dictionary) {
        if (compressionLevel < 0 || compressionLevel > 9) {
            throw new IllegalArgumentException("compressionLevel: " + compressionLevel + " (expected: 0-9)");
        }
        if (windowBits < 9 || windowBits > 15) {
            throw new IllegalArgumentException("windowBits: " + windowBits + " (expected: 9-15)");
        }
        if (memLevel < 1 || memLevel > 9) {
            throw new IllegalArgumentException("memLevel: " + memLevel + " (expected: 1-9)");
        }
        if (dictionary == null) {
            throw new NullPointerException("dictionary");
        }
        ZStream zStream = this.z;
        synchronized (zStream) {
            int resultCode = this.z.deflateInit(compressionLevel, windowBits, memLevel, JZlib.W_ZLIB);
            if (resultCode != 0) {
                ZlibUtil.fail(this.z, "initialization failure", resultCode);
            } else {
                resultCode = this.z.deflateSetDictionary(dictionary, dictionary.length);
                if (resultCode != 0) {
                    ZlibUtil.fail(this.z, "failed to set the dictionary", resultCode);
                }
            }
        }
    }

    @Override
    public ChannelFuture close() {
        return this.close(this.ctx().channel().newFuture());
    }

    @Override
    public ChannelFuture close(ChannelFuture future) {
        return this.finishEncode(this.ctx(), future);
    }

    private ChannelHandlerContext ctx() {
        ChannelHandlerContext ctx = this.ctx;
        if (ctx == null) {
            throw new IllegalStateException("not added to a pipeline");
        }
        return ctx;
    }

    @Override
    public boolean isClosed() {
        return this.finished.get();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
        if (this.finished.get()) {
            return;
        }
        ZStream zStream = this.z;
        synchronized (zStream) {
            block17 : {
                try {
                    int outputLength;
                    int resultCode;
                    int inputLength = in.readableBytes();
                    boolean inHasArray = in.hasArray();
                    this.z.avail_in = inputLength;
                    if (inHasArray) {
                        this.z.next_in = in.array();
                        this.z.next_in_index = in.arrayOffset() + in.readerIndex();
                    } else {
                        byte[] array = new byte[inputLength];
                        in.readBytes(array);
                        this.z.next_in = array;
                        this.z.next_in_index = 0;
                    }
                    int oldNextInIndex = this.z.next_in_index;
                    int maxOutputLength = (int)Math.ceil((double)inputLength * 1.001) + 12;
                    boolean outHasArray = out.hasArray();
                    this.z.avail_out = maxOutputLength;
                    if (outHasArray) {
                        out.ensureWritableBytes(maxOutputLength);
                        this.z.next_out = out.array();
                        this.z.next_out_index = out.arrayOffset() + out.writerIndex();
                    } else {
                        this.z.next_out = new byte[maxOutputLength];
                        this.z.next_out_index = 0;
                    }
                    int oldNextOutIndex = this.z.next_out_index;
                    try {
                        resultCode = this.z.deflate(2);
                    }
                    finally {
                        if (inHasArray) {
                            in.skipBytes(this.z.next_in_index - oldNextInIndex);
                        }
                    }
                    if (resultCode != 0) {
                        ZlibUtil.fail(this.z, "compression failure", resultCode);
                    }
                    if ((outputLength = this.z.next_out_index - oldNextOutIndex) <= 0) break block17;
                    if (outHasArray) {
                        out.writerIndex(out.writerIndex() + outputLength);
                        break block17;
                    }
                    out.writeBytes(this.z.next_out, 0, outputLength);
                }
                finally {
                    this.z.next_in = null;
                    this.z.next_out = null;
                }
            }
        }
    }

    @Override
    public void close(final ChannelHandlerContext ctx, final ChannelFuture future) throws Exception {
        ChannelFuture f = this.finishEncode(ctx, ctx.newFuture());
        f.addListener(new ChannelFutureListener(){

            @Override
            public void operationComplete(ChannelFuture f) throws Exception {
                ctx.close(future);
            }
        });
        if (!f.isDone()) {
            ctx.executor().schedule(new Runnable(){

                @Override
                public void run() {
                    ctx.close(future);
                }
            }, 10, TimeUnit.SECONDS);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private ChannelFuture finishEncode(ChannelHandlerContext ctx, ChannelFuture future) {
        ByteBuf footer;
        if (!this.finished.compareAndSet(false, true)) {
            future.setSuccess();
            return future;
        }
        ZStream zStream = this.z;
        synchronized (zStream) {
            try {
                byte[] out;
                this.z.next_in = EMPTY_ARRAY;
                this.z.next_in_index = 0;
                this.z.avail_in = 0;
                this.z.next_out = out = new byte[32];
                this.z.next_out_index = 0;
                this.z.avail_out = out.length;
                int resultCode = this.z.deflate(4);
                if (resultCode != 0 && resultCode != 1) {
                    future.setFailure(ZlibUtil.exception(this.z, "compression failure", resultCode));
                    ChannelFuture channelFuture = future;
                    return channelFuture;
                }
                footer = this.z.next_out_index != 0 ? Unpooled.wrappedBuffer(out, 0, this.z.next_out_index) : Unpooled.EMPTY_BUFFER;
            }
            finally {
                this.z.deflateEnd();
                this.z.next_in = null;
                this.z.next_out = null;
            }
        }
        ctx.write(footer, future);
        return future;
    }

    @Override
    public void beforeAdd(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
    }

}

