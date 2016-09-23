
package io.netty.handler.codec.compression;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventExecutor;
import io.netty.handler.codec.compression.ZlibEncoder;
import io.netty.handler.codec.compression.ZlibWrapper;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

public class JdkZlibEncoder
extends ZlibEncoder {
    private final byte[] encodeBuf = new byte[8192];
    private final Deflater deflater;
    private final AtomicBoolean finished = new AtomicBoolean();
    private volatile ChannelHandlerContext ctx;
    private final boolean gzip;
    private final CRC32 crc = new CRC32();
    private static final byte[] gzipHeader = new byte[]{31, -117, 8, 0, 0, 0, 0, 0, 0, 0};
    private boolean writeHeader = true;

    public JdkZlibEncoder() {
        this(6);
    }

    public JdkZlibEncoder(int compressionLevel) {
        this(ZlibWrapper.ZLIB, compressionLevel);
    }

    public JdkZlibEncoder(ZlibWrapper wrapper) {
        this(wrapper, 6);
    }

    public JdkZlibEncoder(ZlibWrapper wrapper, int compressionLevel) {
        if (compressionLevel < 0 || compressionLevel > 9) {
            throw new IllegalArgumentException("compressionLevel: " + compressionLevel + " (expected: 0-9)");
        }
        if (wrapper == null) {
            throw new NullPointerException("wrapper");
        }
        if (wrapper == ZlibWrapper.ZLIB_OR_NONE) {
            throw new IllegalArgumentException("wrapper '" + (Object)((Object)ZlibWrapper.ZLIB_OR_NONE) + "' is not " + "allowed for compression.");
        }
        this.gzip = wrapper == ZlibWrapper.GZIP;
        this.deflater = new Deflater(compressionLevel, wrapper != ZlibWrapper.ZLIB);
    }

    public JdkZlibEncoder(byte[] dictionary) {
        this(6, dictionary);
    }

    public JdkZlibEncoder(int compressionLevel, byte[] dictionary) {
        if (compressionLevel < 0 || compressionLevel > 9) {
            throw new IllegalArgumentException("compressionLevel: " + compressionLevel + " (expected: 0-9)");
        }
        if (dictionary == null) {
            throw new NullPointerException("dictionary");
        }
        this.gzip = false;
        this.deflater = new Deflater(compressionLevel);
        this.deflater.setDictionary(dictionary);
    }

    @Override
    public ChannelFuture close() {
        return this.close(this.ctx().newFuture());
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
            out.writeBytes(in);
            in.discardReadBytes();
            return;
        }
        ByteBuf uncompressed = in;
        byte[] inAry = new byte[uncompressed.readableBytes()];
        uncompressed.readBytes(inAry);
        int sizeEstimate = (int)Math.ceil((double)inAry.length * 1.001) + 12;
        out.ensureWritableBytes(sizeEstimate);
        Deflater deflater = this.deflater;
        synchronized (deflater) {
            if (this.gzip) {
                this.crc.update(inAry);
                if (this.writeHeader) {
                    out.writeBytes(gzipHeader);
                    this.writeHeader = false;
                }
            }
            this.deflater.setInput(inAry);
            while (!this.deflater.needsInput()) {
                int numBytes = this.deflater.deflate(this.encodeBuf, 0, this.encodeBuf.length, 2);
                out.writeBytes(this.encodeBuf, 0, numBytes);
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
     */
    private ChannelFuture finishEncode(ChannelHandlerContext ctx, ChannelFuture future) {
        if (!this.finished.compareAndSet(false, true)) {
            future.setSuccess();
            return future;
        }
        ByteBuf footer = Unpooled.buffer();
        Deflater deflater = this.deflater;
        synchronized (deflater) {
            this.deflater.finish();
            while (!this.deflater.finished()) {
                int numBytes = this.deflater.deflate(this.encodeBuf, 0, this.encodeBuf.length);
                footer.writeBytes(this.encodeBuf, 0, numBytes);
            }
            if (this.gzip) {
                int crcValue = (int)this.crc.getValue();
                int uncBytes = this.deflater.getTotalIn();
                footer.writeByte(crcValue);
                footer.writeByte(crcValue >>> 8);
                footer.writeByte(crcValue >>> 16);
                footer.writeByte(crcValue >>> 24);
                footer.writeByte(uncBytes);
                footer.writeByte(uncBytes >>> 8);
                footer.writeByte(uncBytes >>> 16);
                footer.writeByte(uncBytes >>> 24);
            }
            this.deflater.end();
        }
        ctx.nextOutboundByteBuffer().writeBytes(footer);
        ctx.flush(future);
        return future;
    }

    @Override
    public void beforeAdd(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
    }

}

