
package io.netty.util.internal.jzlib;

import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import io.netty.util.internal.jzlib.Adler32;
import io.netty.util.internal.jzlib.CRC32;
import io.netty.util.internal.jzlib.Deflate;
import io.netty.util.internal.jzlib.Inflate;
import io.netty.util.internal.jzlib.JZlib;

public final class ZStream {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ZStream.class);
    public byte[] next_in;
    public int next_in_index;
    public int avail_in;
    public long total_in;
    public byte[] next_out;
    public int next_out_index;
    public int avail_out;
    public long total_out;
    public String msg;
    Deflate dstate;
    Inflate istate;
    long adler;
    int crc32;

    public int inflateInit() {
        return this.inflateInit(15);
    }

    public int inflateInit(Enum<?> wrapperType) {
        return this.inflateInit(15, wrapperType);
    }

    public int inflateInit(int w) {
        return this.inflateInit(w, JZlib.WrapperType.ZLIB);
    }

    public int inflateInit(int w, Enum wrapperType) {
        this.istate = new Inflate();
        return this.istate.inflateInit(this, w, (JZlib.WrapperType)wrapperType);
    }

    public int inflate(int f) {
        if (this.istate == null) {
            return -2;
        }
        return this.istate.inflate(this, f);
    }

    public int inflateEnd() {
        if (this.istate == null) {
            return -2;
        }
        int ret = this.istate.inflateEnd(this);
        this.istate = null;
        return ret;
    }

    public int inflateSync() {
        if (this.istate == null) {
            return -2;
        }
        return this.istate.inflateSync(this);
    }

    public int inflateSetDictionary(byte[] dictionary, int dictLength) {
        if (this.istate == null) {
            return -2;
        }
        return Inflate.inflateSetDictionary(this, dictionary, dictLength);
    }

    public int deflateInit(int level) {
        return this.deflateInit(level, 15);
    }

    public int deflateInit(int level, Enum<?> wrapperType) {
        return this.deflateInit(level, 15, wrapperType);
    }

    public int deflateInit(int level, int bits) {
        return this.deflateInit(level, bits, JZlib.WrapperType.ZLIB);
    }

    public int deflateInit(int level, int bits, Enum<?> wrapperType) {
        return this.deflateInit(level, bits, 8, wrapperType);
    }

    public int deflateInit(int level, int bits, int memLevel, Enum wrapperType) {
        this.dstate = new Deflate();
        return this.dstate.deflateInit(this, level, bits, memLevel, (JZlib.WrapperType)wrapperType);
    }

    public int deflate(int flush) {
        if (this.dstate == null) {
            return -2;
        }
        return this.dstate.deflate(this, flush);
    }

    public int deflateEnd() {
        if (this.dstate == null) {
            return -2;
        }
        int ret = this.dstate.deflateEnd();
        this.dstate = null;
        return ret;
    }

    public int deflateParams(int level, int strategy) {
        if (this.dstate == null) {
            return -2;
        }
        return this.dstate.deflateParams(this, level, strategy);
    }

    public int deflateSetDictionary(byte[] dictionary, int dictLength) {
        if (this.dstate == null) {
            return -2;
        }
        return this.dstate.deflateSetDictionary(this, dictionary, dictLength);
    }

    void flush_pending() {
        int len = this.dstate.pending;
        if (len > this.avail_out) {
            len = this.avail_out;
        }
        if (len == 0) {
            return;
        }
        if (this.dstate.pending_buf.length <= this.dstate.pending_out || this.next_out.length <= this.next_out_index || this.dstate.pending_buf.length < this.dstate.pending_out + len || this.next_out.length < this.next_out_index + len) {
            logger.debug("" + this.dstate.pending_buf.length + ", " + this.dstate.pending_out + ", " + this.next_out.length + ", " + this.next_out_index + ", " + len);
            logger.debug("avail_out=" + this.avail_out);
        }
        System.arraycopy(this.dstate.pending_buf, this.dstate.pending_out, this.next_out, this.next_out_index, len);
        this.next_out_index += len;
        this.dstate.pending_out += len;
        this.total_out += (long)len;
        this.avail_out -= len;
        this.dstate.pending -= len;
        if (this.dstate.pending == 0) {
            this.dstate.pending_out = 0;
        }
    }

    int read_buf(byte[] buf, int start, int size) {
        int len = this.avail_in;
        if (len > size) {
            len = size;
        }
        if (len == 0) {
            return 0;
        }
        this.avail_in -= len;
        switch (this.dstate.wrapperType) {
            case ZLIB: {
                this.adler = Adler32.adler32(this.adler, this.next_in, this.next_in_index, len);
                break;
            }
            case GZIP: {
                this.crc32 = CRC32.crc32(this.crc32, this.next_in, this.next_in_index, len);
            }
        }
        System.arraycopy(this.next_in, this.next_in_index, buf, start, len);
        this.next_in_index += len;
        this.total_in += (long)len;
        return len;
    }

    public void free() {
        this.next_in = null;
        this.next_out = null;
        this.msg = null;
    }

}

