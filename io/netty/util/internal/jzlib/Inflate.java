
package io.netty.util.internal.jzlib;

import io.netty.util.internal.jzlib.Adler32;
import io.netty.util.internal.jzlib.CRC32;
import io.netty.util.internal.jzlib.InfBlocks;
import io.netty.util.internal.jzlib.JZlib;
import io.netty.util.internal.jzlib.ZStream;

final class Inflate {
    private static final int METHOD = 0;
    private static final int FLAG = 1;
    private static final int DICT4 = 2;
    private static final int DICT3 = 3;
    private static final int DICT2 = 4;
    private static final int DICT1 = 5;
    private static final int DICT0 = 6;
    private static final int BLOCKS = 7;
    private static final int CHECK4 = 8;
    private static final int CHECK3 = 9;
    private static final int CHECK2 = 10;
    private static final int CHECK1 = 11;
    private static final int DONE = 12;
    private static final int BAD = 13;
    private static final int GZIP_ID1 = 14;
    private static final int GZIP_ID2 = 15;
    private static final int GZIP_CM = 16;
    private static final int GZIP_FLG = 17;
    private static final int GZIP_MTIME_XFL_OS = 18;
    private static final int GZIP_XLEN = 19;
    private static final int GZIP_FEXTRA = 20;
    private static final int GZIP_FNAME = 21;
    private static final int GZIP_FCOMMENT = 22;
    private static final int GZIP_FHCRC = 23;
    private static final int GZIP_CRC32 = 24;
    private static final int GZIP_ISIZE = 25;
    private int mode;
    private int method;
    private final long[] was = new long[1];
    private long need;
    private int marker;
    private JZlib.WrapperType wrapperType;
    private int wbits;
    private InfBlocks blocks;
    private int gzipFlag;
    private int gzipBytesToRead;
    private int gzipXLen;
    private int gzipUncompressedBytes;
    private int gzipCRC32;
    private int gzipISize;
    private static final byte[] mark = new byte[]{0, 0, -1, -1};

    Inflate() {
    }

    private int inflateReset(ZStream z) {
        if (z == null || z.istate == null) {
            return -2;
        }
        z.total_out = 0;
        z.total_in = 0;
        z.msg = null;
        switch (this.wrapperType) {
            case NONE: {
                z.istate.mode = 7;
                break;
            }
            case ZLIB: 
            case ZLIB_OR_NONE: {
                z.istate.mode = 0;
                break;
            }
            case GZIP: {
                z.istate.mode = 14;
            }
        }
        z.istate.blocks.reset(z, null);
        this.gzipUncompressedBytes = 0;
        return 0;
    }

    int inflateEnd(ZStream z) {
        if (this.blocks != null) {
            this.blocks.free(z);
        }
        this.blocks = null;
        return 0;
    }

    int inflateInit(ZStream z, int w, JZlib.WrapperType wrapperType) {
        z.msg = null;
        this.blocks = null;
        this.wrapperType = wrapperType;
        if (w < 0) {
            throw new IllegalArgumentException("w: " + w);
        }
        if (w < 8 || w > 15) {
            this.inflateEnd(z);
            return -2;
        }
        this.wbits = w;
        z.istate.blocks = new InfBlocks(z, z.istate.wrapperType == JZlib.WrapperType.NONE ? null : this, 1 << w);
        this.inflateReset(z);
        return 0;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Unable to fully structure code
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     * Lifted jumps to return sites
     */
    int inflate(ZStream z, int f) {
        if (z == null) return -2;
        if (z.istate == null) return -2;
        if (z.next_in == null) {
            return -2;
        }
        f = f == 4 ? -5 : 0;
        r = -5;
        block33 : do {
            switch (z.istate.mode) {
                case 0: {
                    if (z.avail_in == 0) {
                        return r;
                    }
                    if (z.istate.wrapperType == JZlib.WrapperType.ZLIB_OR_NONE) {
                        if ((z.next_in[z.next_in_index] & 15) != 8 || (z.next_in[z.next_in_index] >> 4) + 8 > z.istate.wbits) {
                            z.istate.wrapperType = JZlib.WrapperType.NONE;
                            z.istate.mode = 7;
                            continue block33;
                        }
                        z.istate.wrapperType = JZlib.WrapperType.ZLIB;
                    }
                    r = f;
                    --z.avail_in;
                    ++z.total_in;
                    if (((z.istate.method = z.next_in[z.next_in_index++]) & 15) != 8) {
                        z.istate.mode = 13;
                        z.msg = "unknown compression method";
                        z.istate.marker = 5;
                        continue block33;
                    }
                    if ((z.istate.method >> 4) + 8 > z.istate.wbits) {
                        z.istate.mode = 13;
                        z.msg = "invalid window size";
                        z.istate.marker = 5;
                        continue block33;
                    }
                    z.istate.mode = 1;
                }
                case 1: {
                    if (z.avail_in == 0) {
                        return r;
                    }
                    r = f;
                    --z.avail_in;
                    ++z.total_in;
                    if (((z.istate.method << 8) + (b = z.next_in[z.next_in_index++] & 255)) % 31 != 0) {
                        z.istate.mode = 13;
                        z.msg = "incorrect header check";
                        z.istate.marker = 5;
                        continue block33;
                    }
                    if ((b & 32) == 0) {
                        z.istate.mode = 7;
                        continue block33;
                    }
                    z.istate.mode = 2;
                }
                case 2: {
                    if (z.avail_in == 0) {
                        return r;
                    }
                    r = f;
                    --z.avail_in;
                    ++z.total_in;
                    z.istate.need = (long)((z.next_in[z.next_in_index++] & 255) << 24) & 0xFF000000L;
                    z.istate.mode = 3;
                }
                case 3: {
                    if (z.avail_in == 0) {
                        return r;
                    }
                    r = f;
                    --z.avail_in;
                    ++z.total_in;
                    z.istate.need += (long)((z.next_in[z.next_in_index++] & 255) << 16) & 0xFF0000;
                    z.istate.mode = 4;
                }
                case 4: {
                    if (z.avail_in == 0) {
                        return r;
                    }
                    r = f;
                    --z.avail_in;
                    ++z.total_in;
                    z.istate.need += (long)((z.next_in[z.next_in_index++] & 255) << 8) & 65280;
                    z.istate.mode = 5;
                }
                case 5: {
                    if (z.avail_in == 0) {
                        return r;
                    }
                    --z.avail_in;
                    ++z.total_in;
                    z.istate.need += (long)z.next_in[z.next_in_index++] & 255;
                    z.adler = z.istate.need;
                    z.istate.mode = 6;
                    return 2;
                }
                case 6: {
                    z.istate.mode = 13;
                    z.msg = "need dictionary";
                    z.istate.marker = 0;
                    return -2;
                }
                case 7: {
                    old_next_out_index = z.next_out_index;
                    try {
                        r = z.istate.blocks.proc(z, r);
                        if (r == -3) {
                            z.istate.mode = 13;
                            z.istate.marker = 0;
                            continue block33;
                        }
                        if (r == 0) {
                            r = f;
                        }
                        if (r != 1) {
                            decompressedBytes = r;
                            return decompressedBytes;
                        }
                        r = f;
                        z.istate.blocks.reset(z, z.istate.was);
                    }
                    finally {
                        decompressedBytes = z.next_out_index - old_next_out_index;
                        this.gzipUncompressedBytes += decompressedBytes;
                        z.crc32 = CRC32.crc32(z.crc32, z.next_out, old_next_out_index, decompressedBytes);
                        continue block33;
                    }
                    if (z.istate.wrapperType == JZlib.WrapperType.NONE) {
                        z.istate.mode = 12;
                        continue block33;
                    }
                    if (z.istate.wrapperType != JZlib.WrapperType.ZLIB) ** GOTO lbl112
                    z.istate.mode = 8;
                    ** GOTO lbl122
lbl112: // 1 sources:
                    if (z.istate.wrapperType == JZlib.WrapperType.GZIP) {
                        this.gzipCRC32 = 0;
                        this.gzipISize = 0;
                        this.gzipBytesToRead = 4;
                        z.istate.mode = 24;
                        continue block33;
                    }
                    z.istate.mode = 13;
                    z.msg = "unexpected state";
                    z.istate.marker = 0;
                    continue block33;
                }
lbl122: // 2 sources:
                case 8: {
                    if (z.avail_in == 0) {
                        return r;
                    }
                    r = f;
                    --z.avail_in;
                    ++z.total_in;
                    z.istate.need = (long)((z.next_in[z.next_in_index++] & 255) << 24) & 0xFF000000L;
                    z.istate.mode = 9;
                }
                case 9: {
                    if (z.avail_in == 0) {
                        return r;
                    }
                    r = f;
                    --z.avail_in;
                    ++z.total_in;
                    z.istate.need += (long)((z.next_in[z.next_in_index++] & 255) << 16) & 0xFF0000;
                    z.istate.mode = 10;
                }
                case 10: {
                    if (z.avail_in == 0) {
                        return r;
                    }
                    r = f;
                    --z.avail_in;
                    ++z.total_in;
                    z.istate.need += (long)((z.next_in[z.next_in_index++] & 255) << 8) & 65280;
                    z.istate.mode = 11;
                }
                case 11: {
                    if (z.avail_in == 0) {
                        return r;
                    }
                    r = f;
                    --z.avail_in;
                    ++z.total_in;
                    z.istate.need += (long)z.next_in[z.next_in_index++] & 255;
                    if ((int)z.istate.was[0] != (int)z.istate.need) {
                        z.istate.mode = 13;
                        z.msg = "incorrect data check";
                        z.istate.marker = 5;
                        continue block33;
                    }
                    z.istate.mode = 12;
                }
                case 12: {
                    return 1;
                }
                case 13: {
                    return -3;
                }
                case 14: {
                    if (z.avail_in == 0) {
                        return r;
                    }
                    r = f;
                    --z.avail_in;
                    ++z.total_in;
                    if ((z.next_in[z.next_in_index++] & 255) != 31) {
                        z.istate.mode = 13;
                        z.msg = "not a gzip stream";
                        z.istate.marker = 5;
                        continue block33;
                    }
                    z.istate.mode = 15;
                }
                case 15: {
                    if (z.avail_in == 0) {
                        return r;
                    }
                    r = f;
                    --z.avail_in;
                    ++z.total_in;
                    if ((z.next_in[z.next_in_index++] & 255) != 139) {
                        z.istate.mode = 13;
                        z.msg = "not a gzip stream";
                        z.istate.marker = 5;
                        continue block33;
                    }
                    z.istate.mode = 16;
                }
                case 16: {
                    if (z.avail_in == 0) {
                        return r;
                    }
                    r = f;
                    --z.avail_in;
                    ++z.total_in;
                    if ((z.next_in[z.next_in_index++] & 255) != 8) {
                        z.istate.mode = 13;
                        z.msg = "unknown compression method";
                        z.istate.marker = 5;
                        continue block33;
                    }
                    z.istate.mode = 17;
                }
                case 17: {
                    if (z.avail_in == 0) {
                        return r;
                    }
                    r = f;
                    --z.avail_in;
                    ++z.total_in;
                    this.gzipFlag = z.next_in[z.next_in_index++] & 255;
                    if ((this.gzipFlag & 226) != 0) {
                        z.istate.mode = 13;
                        z.msg = "unsupported flag";
                        z.istate.marker = 5;
                        continue block33;
                    }
                    this.gzipBytesToRead = 6;
                    z.istate.mode = 18;
                }
                case 18: {
                    ** break;
                }
                case 24: {
                    ** GOTO lbl291
                }
                default: {
                    return -2;
                }
lbl219: // 2 sources:
                while (this.gzipBytesToRead > 0) {
                    if (z.avail_in == 0) {
                        return r;
                    }
                    r = f;
                    --z.avail_in;
                    ++z.total_in;
                    ++z.next_in_index;
                    --this.gzipBytesToRead;
                }
                z.istate.mode = 19;
                this.gzipXLen = 0;
                this.gzipBytesToRead = 2;
                case 19: {
                    if ((this.gzipFlag & 4) == 0) {
                        z.istate.mode = 21;
                        continue block33;
                    }
                    while (this.gzipBytesToRead > 0) {
                        if (z.avail_in == 0) {
                            return r;
                        }
                        r = f;
                        --z.avail_in;
                        ++z.total_in;
                        this.gzipXLen |= (z.next_in[z.next_in_index++] & 255) << (1 - this.gzipBytesToRead) * 8;
                        --this.gzipBytesToRead;
                    }
                    this.gzipBytesToRead = this.gzipXLen;
                    z.istate.mode = 20;
                }
                case 20: {
                    while (this.gzipBytesToRead > 0) {
                        if (z.avail_in == 0) {
                            return r;
                        }
                        r = f;
                        --z.avail_in;
                        ++z.total_in;
                        ++z.next_in_index;
                        --this.gzipBytesToRead;
                    }
                    z.istate.mode = 21;
                }
                case 21: {
                    if ((this.gzipFlag & 8) != 0) {
                        do {
                            if (z.avail_in == 0) {
                                return r;
                            }
                            r = f;
                            --z.avail_in;
                            ++z.total_in;
                        } while (z.next_in[z.next_in_index++] != 0);
                    }
                    z.istate.mode = 22;
                }
                case 22: {
                    if ((this.gzipFlag & 16) != 0) {
                        do {
                            if (z.avail_in == 0) {
                                return r;
                            }
                            r = f;
                            --z.avail_in;
                            ++z.total_in;
                        } while (z.next_in[z.next_in_index++] != 0);
                    }
                    this.gzipBytesToRead = 2;
                    z.istate.mode = 23;
                }
                case 23: {
                    if ((this.gzipFlag & 2) != 0) {
                        while (this.gzipBytesToRead > 0) {
                            if (z.avail_in == 0) {
                                return r;
                            }
                            r = f;
                            --z.avail_in;
                            ++z.total_in;
                            ++z.next_in_index;
                            --this.gzipBytesToRead;
                        }
                    }
                    z.istate.mode = 7;
                    continue block33;
                }
lbl291: // 2 sources:
                while (this.gzipBytesToRead > 0) {
                    if (z.avail_in == 0) {
                        return r;
                    }
                    r = f;
                    --z.avail_in;
                    ++z.total_in;
                    --this.gzipBytesToRead;
                    z.istate.gzipCRC32 |= (z.next_in[z.next_in_index++] & 255) << (3 - this.gzipBytesToRead) * 8;
                }
                if (z.crc32 != z.istate.gzipCRC32) {
                    z.istate.mode = 13;
                    z.msg = "incorrect CRC32 checksum";
                    z.istate.marker = 5;
                    continue block33;
                }
                this.gzipBytesToRead = 4;
                z.istate.mode = 25;
                case 25: 
            }
            while (this.gzipBytesToRead > 0) {
                if (z.avail_in == 0) {
                    return r;
                }
                r = f;
                --z.avail_in;
                ++z.total_in;
                --this.gzipBytesToRead;
                z.istate.gzipISize |= (z.next_in[z.next_in_index++] & 255) << (3 - this.gzipBytesToRead) * 8;
            }
            if (this.gzipUncompressedBytes != z.istate.gzipISize) {
                z.istate.mode = 13;
                z.msg = "incorrect ISIZE checksum";
                z.istate.marker = 5;
                continue;
            }
            z.istate.mode = 12;
        } while (true);
    }

    static int inflateSetDictionary(ZStream z, byte[] dictionary, int dictLength) {
        int index = 0;
        int length = dictLength;
        if (z == null || z.istate == null || z.istate.mode != 6) {
            return -2;
        }
        if (Adler32.adler32(1, dictionary, 0, dictLength) != z.adler) {
            return -3;
        }
        z.adler = Adler32.adler32(0, null, 0, 0);
        if (length >= 1 << z.istate.wbits) {
            length = (1 << z.istate.wbits) - 1;
            index = dictLength - length;
        }
        z.istate.blocks.set_dictionary(dictionary, index, length);
        z.istate.mode = 7;
        return 0;
    }

    int inflateSync(ZStream z) {
        int n;
        if (z == null || z.istate == null) {
            return -2;
        }
        if (z.istate.mode != 13) {
            z.istate.mode = 13;
            z.istate.marker = 0;
        }
        if ((n = z.avail_in) == 0) {
            return -5;
        }
        int p = z.next_in_index;
        int m = z.istate.marker;
        while (n != 0 && m < 4) {
            m = z.next_in[p] == mark[m] ? ++m : (z.next_in[p] != 0 ? 0 : 4 - m);
            ++p;
            --n;
        }
        z.total_in += (long)(p - z.next_in_index);
        z.next_in_index = p;
        z.avail_in = n;
        z.istate.marker = m;
        if (m != 4) {
            return -3;
        }
        long r = z.total_in;
        long w = z.total_out;
        this.inflateReset(z);
        z.total_in = r;
        z.total_out = w;
        z.istate.mode = 7;
        return 0;
    }

}

