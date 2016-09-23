
package io.netty.util.internal.jzlib;

import io.netty.util.internal.jzlib.Adler32;
import io.netty.util.internal.jzlib.InfCodes;
import io.netty.util.internal.jzlib.InfTree;
import io.netty.util.internal.jzlib.ZStream;

final class InfBlocks {
    private static final int[] inflate_mask = new int[]{0, 1, 3, 7, 15, 31, 63, 127, 255, 511, 1023, 2047, 4095, 8191, 16383, 32767, 65535};
    private static final int[] border = new int[]{16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15};
    private static final int TYPE = 0;
    private static final int LENS = 1;
    private static final int STORED = 2;
    private static final int TABLE = 3;
    private static final int BTREE = 4;
    private static final int DTREE = 5;
    private static final int CODES = 6;
    private static final int DRY = 7;
    private static final int DONE = 8;
    private static final int BAD = 9;
    private int mode;
    private int left;
    private int table;
    private int index;
    private int[] blens;
    private final int[] bb = new int[1];
    private final int[] tb = new int[1];
    private final InfCodes codes = new InfCodes();
    private int last;
    int bitk;
    int bitb;
    private int[] hufts = new int[4320];
    byte[] window;
    final int end;
    int read;
    int write;
    private final Object checkfn;
    private long check;
    private final InfTree inftree = new InfTree();

    InfBlocks(ZStream z, Object checkfn, int w) {
        this.window = new byte[w];
        this.end = w;
        this.checkfn = checkfn;
        this.mode = 0;
        this.reset(z, null);
    }

    void reset(ZStream z, long[] c) {
        if (c != null) {
            c[0] = this.check;
        }
        this.mode = 0;
        this.bitk = 0;
        this.bitb = 0;
        this.write = 0;
        this.read = 0;
        if (this.checkfn != null) {
            z.adler = this.check = Adler32.adler32(0, null, 0, 0);
        }
    }

    int proc(ZStream z, int r) {
        int p = z.next_in_index;
        int n = z.avail_in;
        int b = this.bitb;
        int k = this.bitk;
        int q = this.write;
        int m = q < this.read ? this.read - q - 1 : this.end - q;
        block18 : do {
            switch (this.mode) {
                int[][] td;
                int[][] tl;
                int[] bl;
                int t;
                int[] bd;
                case 0: {
                    while (k < 3) {
                        if (n == 0) {
                            this.bitb = b;
                            this.bitk = k;
                            z.avail_in = n;
                            z.total_in += (long)(p - z.next_in_index);
                            z.next_in_index = p;
                            this.write = q;
                            return this.inflate_flush(z, r);
                        }
                        r = 0;
                        --n;
                        b |= (z.next_in[p++] & 255) << k;
                        k += 8;
                    }
                    t = b & 7;
                    this.last = t & 1;
                    switch (t >>> 1) {
                        case 0: {
                            b >>>= 3;
                            t = (k -= 3) & 7;
                            b >>>= t;
                            k -= t;
                            this.mode = 1;
                            break;
                        }
                        case 1: {
                            bl = new int[1];
                            bd = new int[1];
                            tl = new int[1][];
                            td = new int[1][];
                            InfTree.inflate_trees_fixed(bl, bd, tl, td);
                            this.codes.init(bl[0], bd[0], tl[0], 0, td[0], 0);
                            b >>>= 3;
                            k -= 3;
                            this.mode = 6;
                            break;
                        }
                        case 2: {
                            b >>>= 3;
                            k -= 3;
                            this.mode = 3;
                            break;
                        }
                        case 3: {
                            this.mode = 9;
                            z.msg = "invalid block type";
                            r = -3;
                            this.bitb = b >>>= 3;
                            this.bitk = k -= 3;
                            z.avail_in = n;
                            z.total_in += (long)(p - z.next_in_index);
                            z.next_in_index = p;
                            this.write = q;
                            return this.inflate_flush(z, r);
                        }
                    }
                    continue block18;
                }
                case 1: {
                    while (k < 32) {
                        if (n == 0) {
                            this.bitb = b;
                            this.bitk = k;
                            z.avail_in = n;
                            z.total_in += (long)(p - z.next_in_index);
                            z.next_in_index = p;
                            this.write = q;
                            return this.inflate_flush(z, r);
                        }
                        r = 0;
                        --n;
                        b |= (z.next_in[p++] & 255) << k;
                        k += 8;
                    }
                    if ((~ b >>> 16 & 65535) != (b & 65535)) {
                        this.mode = 9;
                        z.msg = "invalid stored block lengths";
                        r = -3;
                        this.bitb = b;
                        this.bitk = k;
                        z.avail_in = n;
                        z.total_in += (long)(p - z.next_in_index);
                        z.next_in_index = p;
                        this.write = q;
                        return this.inflate_flush(z, r);
                    }
                    this.left = b & 65535;
                    k = 0;
                    b = 0;
                    this.mode = this.left != 0 ? 2 : (this.last != 0 ? 7 : 0);
                    continue block18;
                }
                case 2: {
                    if (n == 0) {
                        this.bitb = b;
                        this.bitk = k;
                        z.avail_in = 0;
                        z.total_in += (long)(p - z.next_in_index);
                        z.next_in_index = p;
                        this.write = q;
                        return this.inflate_flush(z, r);
                    }
                    if (m == 0) {
                        if (q == this.end && this.read != 0) {
                            q = 0;
                            int n2 = m = q < this.read ? this.read - q - 1 : this.end - q;
                        }
                        if (m == 0) {
                            this.write = q;
                            r = this.inflate_flush(z, r);
                            q = this.write;
                            int n3 = m = q < this.read ? this.read - q - 1 : this.end - q;
                            if (q == this.end && this.read != 0) {
                                q = 0;
                                int n4 = m = q < this.read ? this.read - q - 1 : this.end - q;
                            }
                            if (m == 0) {
                                this.bitb = b;
                                this.bitk = k;
                                z.avail_in = n;
                                z.total_in += (long)(p - z.next_in_index);
                                z.next_in_index = p;
                                this.write = q;
                                return this.inflate_flush(z, r);
                            }
                        }
                    }
                    r = 0;
                    t = this.left;
                    if (t > n) {
                        t = n;
                    }
                    if (t > m) {
                        t = m;
                    }
                    System.arraycopy(z.next_in, p, this.window, q, t);
                    p += t;
                    n -= t;
                    q += t;
                    m -= t;
                    if ((this.left -= t) != 0) continue block18;
                    this.mode = this.last != 0 ? 7 : 0;
                    continue block18;
                }
                case 3: {
                    while (k < 14) {
                        if (n == 0) {
                            this.bitb = b;
                            this.bitk = k;
                            z.avail_in = n;
                            z.total_in += (long)(p - z.next_in_index);
                            z.next_in_index = p;
                            this.write = q;
                            return this.inflate_flush(z, r);
                        }
                        r = 0;
                        --n;
                        b |= (z.next_in[p++] & 255) << k;
                        k += 8;
                    }
                    this.table = t = b & 16383;
                    if ((t & 31) > 29 || (t >> 5 & 31) > 29) {
                        this.mode = 9;
                        z.msg = "too many length or distance symbols";
                        r = -3;
                        this.bitb = b;
                        this.bitk = k;
                        z.avail_in = n;
                        z.total_in += (long)(p - z.next_in_index);
                        z.next_in_index = p;
                        this.write = q;
                        return this.inflate_flush(z, r);
                    }
                    t = 258 + (t & 31) + (t >> 5 & 31);
                    if (this.blens == null || this.blens.length < t) {
                        this.blens = new int[t];
                    } else {
                        for (int i = 0; i < t; ++i) {
                            this.blens[i] = 0;
                        }
                    }
                    b >>>= 14;
                    k -= 14;
                    this.index = 0;
                    this.mode = 4;
                }
                case 4: {
                    while (this.index < 4 + (this.table >>> 10)) {
                        while (k < 3) {
                            if (n == 0) {
                                this.bitb = b;
                                this.bitk = k;
                                z.avail_in = n;
                                z.total_in += (long)(p - z.next_in_index);
                                z.next_in_index = p;
                                this.write = q;
                                return this.inflate_flush(z, r);
                            }
                            r = 0;
                            --n;
                            b |= (z.next_in[p++] & 255) << k;
                            k += 8;
                        }
                        this.blens[InfBlocks.border[this.index++]] = b & 7;
                        b >>>= 3;
                        k -= 3;
                    }
                    while (this.index < 19) {
                        this.blens[InfBlocks.border[this.index++]] = 0;
                    }
                    this.bb[0] = 7;
                    t = this.inftree.inflate_trees_bits(this.blens, this.bb, this.tb, this.hufts, z);
                    if (t != 0) {
                        r = t;
                        if (r == -3) {
                            this.blens = null;
                            this.mode = 9;
                        }
                        this.bitb = b;
                        this.bitk = k;
                        z.avail_in = n;
                        z.total_in += (long)(p - z.next_in_index);
                        z.next_in_index = p;
                        this.write = q;
                        return this.inflate_flush(z, r);
                    }
                    this.index = 0;
                    this.mode = 5;
                }
                case 5: {
                    while (this.index < 258 + ((t = this.table) & 31) + (t >> 5 & 31)) {
                        int c;
                        int j;
                        t = this.bb[0];
                        while (k < t) {
                            if (n == 0) {
                                this.bitb = b;
                                this.bitk = k;
                                z.avail_in = n;
                                z.total_in += (long)(p - z.next_in_index);
                                z.next_in_index = p;
                                this.write = q;
                                return this.inflate_flush(z, r);
                            }
                            r = 0;
                            --n;
                            b |= (z.next_in[p++] & 255) << k;
                            k += 8;
                        }
                        if (this.tb[0] == -1) {
                            // empty if block
                        }
                        if ((c = this.hufts[(this.tb[0] + (b & inflate_mask[t = this.hufts[(this.tb[0] + (b & inflate_mask[t])) * 3 + 1]])) * 3 + 2]) < 16) {
                            b >>>= t;
                            k -= t;
                            this.blens[this.index++] = c;
                            continue;
                        }
                        int i = c == 18 ? 7 : c - 14;
                        int n5 = j = c == 18 ? 11 : 3;
                        while (k < t + i) {
                            if (n == 0) {
                                this.bitb = b;
                                this.bitk = k;
                                z.avail_in = n;
                                z.total_in += (long)(p - z.next_in_index);
                                z.next_in_index = p;
                                this.write = q;
                                return this.inflate_flush(z, r);
                            }
                            r = 0;
                            --n;
                            b |= (z.next_in[p++] & 255) << k;
                            k += 8;
                        }
                        k -= t;
                        b >>>= i;
                        k -= i;
                        i = this.index;
                        if (i + (j += (b >>>= t) & inflate_mask[i]) > 258 + ((t = this.table) & 31) + (t >> 5 & 31) || c == 16 && i < 1) {
                            this.blens = null;
                            this.mode = 9;
                            z.msg = "invalid bit length repeat";
                            r = -3;
                            this.bitb = b;
                            this.bitk = k;
                            z.avail_in = n;
                            z.total_in += (long)(p - z.next_in_index);
                            z.next_in_index = p;
                            this.write = q;
                            return this.inflate_flush(z, r);
                        }
                        c = c == 16 ? this.blens[i - 1] : 0;
                        do {
                            this.blens[i++] = c;
                        } while (--j != 0);
                        this.index = i;
                    }
                    this.tb[0] = -1;
                    bl = new int[1];
                    bd = new int[1];
                    tl = new int[1];
                    td = new int[1];
                    bl[0] = 9;
                    bd[0] = 6;
                    t = this.table;
                    t = this.inftree.inflate_trees_dynamic(257 + (t & 31), 1 + (t >> 5 & 31), this.blens, bl, bd, (int[])tl, (int[])td, this.hufts, z);
                    if (t != 0) {
                        if (t == -3) {
                            this.blens = null;
                            this.mode = 9;
                        }
                        r = t;
                        this.bitb = b;
                        this.bitk = k;
                        z.avail_in = n;
                        z.total_in += (long)(p - z.next_in_index);
                        z.next_in_index = p;
                        this.write = q;
                        return this.inflate_flush(z, r);
                    }
                    this.codes.init(bl[0], bd[0], this.hufts, (int)tl[0], this.hufts, (int)td[0]);
                    this.mode = 6;
                }
                case 6: {
                    this.bitb = b;
                    this.bitk = k;
                    z.avail_in = n;
                    z.total_in += (long)(p - z.next_in_index);
                    z.next_in_index = p;
                    this.write = q;
                    r = this.codes.proc(this, z, r);
                    if (r != 1) {
                        return this.inflate_flush(z, r);
                    }
                    r = 0;
                    p = z.next_in_index;
                    n = z.avail_in;
                    b = this.bitb;
                    k = this.bitk;
                    q = this.write;
                    int n6 = m = q < this.read ? this.read - q - 1 : this.end - q;
                    if (this.last == 0) {
                        this.mode = 0;
                        continue block18;
                    }
                    this.mode = 7;
                }
                case 7: {
                    this.write = q;
                    r = this.inflate_flush(z, r);
                    q = this.write;
                    if (this.read != this.write) {
                        this.bitb = b;
                        this.bitk = k;
                        z.avail_in = n;
                        z.total_in += (long)(p - z.next_in_index);
                        z.next_in_index = p;
                        this.write = q;
                        return this.inflate_flush(z, r);
                    }
                    this.mode = 8;
                }
                case 8: {
                    r = 1;
                    this.bitb = b;
                    this.bitk = k;
                    z.avail_in = n;
                    z.total_in += (long)(p - z.next_in_index);
                    z.next_in_index = p;
                    this.write = q;
                    return this.inflate_flush(z, r);
                }
                case 9: {
                    r = -3;
                    this.bitb = b;
                    this.bitk = k;
                    z.avail_in = n;
                    z.total_in += (long)(p - z.next_in_index);
                    z.next_in_index = p;
                    this.write = q;
                    return this.inflate_flush(z, r);
                }
            }
            break;
        } while (true);
        r = -2;
        this.bitb = b;
        this.bitk = k;
        z.avail_in = n;
        z.total_in += (long)(p - z.next_in_index);
        z.next_in_index = p;
        this.write = q;
        return this.inflate_flush(z, r);
    }

    void free(ZStream z) {
        this.reset(z, null);
        this.window = null;
        this.hufts = null;
    }

    void set_dictionary(byte[] d, int start, int n) {
        System.arraycopy(d, start, this.window, 0, n);
        this.read = this.write = n;
    }

    int sync_point() {
        return this.mode == 1 ? 1 : 0;
    }

    int inflate_flush(ZStream z, int r) {
        int p = z.next_out_index;
        int q = this.read;
        int n = (q <= this.write ? this.write : this.end) - q;
        if (n > z.avail_out) {
            n = z.avail_out;
        }
        if (n != 0 && r == -5) {
            r = 0;
        }
        z.avail_out -= n;
        z.total_out += (long)n;
        if (this.checkfn != null) {
            z.adler = this.check = Adler32.adler32(this.check, this.window, q, n);
        }
        System.arraycopy(this.window, q, z.next_out, p, n);
        p += n;
        if ((q += n) == this.end) {
            q = 0;
            if (this.write == this.end) {
                this.write = 0;
            }
            if ((n = this.write - q) > z.avail_out) {
                n = z.avail_out;
            }
            if (n != 0 && r == -5) {
                r = 0;
            }
            z.avail_out -= n;
            z.total_out += (long)n;
            if (this.checkfn != null) {
                z.adler = this.check = Adler32.adler32(this.check, this.window, q, n);
            }
            System.arraycopy(this.window, q, z.next_out, p, n);
            p += n;
            q += n;
        }
        z.next_out_index = p;
        this.read = q;
        return r;
    }
}

