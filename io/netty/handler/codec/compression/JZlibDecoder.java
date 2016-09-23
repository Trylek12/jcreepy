
package io.netty.handler.codec.compression;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.compression.ZlibDecoder;
import io.netty.handler.codec.compression.ZlibUtil;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.util.internal.jzlib.JZlib;
import io.netty.util.internal.jzlib.ZStream;

public class JZlibDecoder
extends ZlibDecoder {
    private final ZStream z = new ZStream();
    private byte[] dictionary;
    private volatile boolean finished;

    public JZlibDecoder() {
        this(ZlibWrapper.ZLIB);
    }

    public JZlibDecoder(ZlibWrapper wrapper) {
        if (wrapper == null) {
            throw new NullPointerException("wrapper");
        }
        int resultCode = this.z.inflateInit(ZlibUtil.convertWrapperType(wrapper));
        if (resultCode != 0) {
            ZlibUtil.fail(this.z, "initialization failure", resultCode);
        }
    }

    public JZlibDecoder(byte[] dictionary) {
        if (dictionary == null) {
            throw new NullPointerException("dictionary");
        }
        this.dictionary = dictionary;
        int resultCode = this.z.inflateInit(JZlib.W_ZLIB);
        if (resultCode != 0) {
            ZlibUtil.fail(this.z, "initialization failure", resultCode);
        }
    }

    @Override
    public boolean isClosed() {
        return this.finished;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
        if (!in.readable()) {
            return;
        }
        try {
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
            int maxOutputLength = inputLength << 1;
            boolean outHasArray = out.hasArray();
            if (!outHasArray) {
                this.z.next_out = new byte[maxOutputLength];
            }
            block12 : do {
                block13 : do {
                    this.z.avail_out = maxOutputLength;
                    if (outHasArray) {
                        out.ensureWritableBytes(maxOutputLength);
                        this.z.next_out = out.array();
                        this.z.next_out_index = out.arrayOffset() + out.writerIndex();
                    } else {
                        this.z.next_out_index = 0;
                    }
                    int oldNextOutIndex = this.z.next_out_index;
                    int resultCode = this.z.inflate(2);
                    int outputLength = this.z.next_out_index - oldNextOutIndex;
                    if (outputLength > 0) {
                        if (outHasArray) {
                            out.writerIndex(out.writerIndex() + outputLength);
                        } else {
                            out.writeBytes(this.z.next_out, 0, outputLength);
                        }
                    }
                    switch (resultCode) {
                        case 2: {
                            if (this.dictionary == null) {
                                ZlibUtil.fail(this.z, "decompression failure", resultCode);
                                continue block12;
                            }
                            resultCode = this.z.inflateSetDictionary(this.dictionary, this.dictionary.length);
                            if (resultCode == 0) continue block12;
                            ZlibUtil.fail(this.z, "failed to set the dictionary", resultCode);
                            continue block12;
                        }
                        case 1: {
                            this.finished = true;
                            this.z.inflateEnd();
                            return;
                        }
                        case 0: {
                            continue block12;
                        }
                        case -5: {
                            if (this.z.avail_in <= 0) return;
                            continue block12;
                        }
                        default: {
                            ZlibUtil.fail(this.z, "decompression failure", resultCode);
                            continue block13;
                        }
                    }
                    break;
                } while (true);
                break;
            } while (true);
            finally {
                if (inHasArray) {
                    in.skipBytes(this.z.next_in_index - oldNextInIndex);
                }
            }
        }
        finally {
            this.z.next_in = null;
            this.z.next_out = null;
        }
    }
}

