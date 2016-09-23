
package io.netty.util.internal.jzlib;

import io.netty.util.internal.jzlib.InfBlocks;
import io.netty.util.internal.jzlib.ZStream;

final class InfCodes {
    private static final int[] inflate_mask = new int[]{0, 1, 3, 7, 15, 31, 63, 127, 255, 511, 1023, 2047, 4095, 8191, 16383, 32767, 65535};
    private static final int START = 0;
    private static final int LEN = 1;
    private static final int LENEXT = 2;
    private static final int DIST = 3;
    private static final int DISTEXT = 4;
    private static final int COPY = 5;
    private static final int LIT = 6;
    private static final int WASH = 7;
    private static final int END = 8;
    private static final int BADCODE = 9;
    private int mode;
    private int len;
    private int[] tree;
    private int tree_index;
    private int need;
    private int lit;
    private int get;
    private int dist;
    private byte lbits;
    private byte dbits;
    private int[] ltree;
    private int ltree_index;
    private int[] dtree;
    private int dtree_index;

    InfCodes() {
    }

    void init(int bl, int bd, int[] tl, int tl_index, int[] td, int td_index) {
        this.mode = 0;
        this.lbits = (byte)bl;
        this.dbits = (byte)bd;
        this.ltree = tl;
        this.ltree_index = tl_index;
        this.dtree = td;
        this.dtree_index = td_index;
        this.tree = null;
    }

    int proc(InfBlocks s, ZStream z, int r) {
        int p = z.next_in_index;
        int n = z.avail_in;
        int b = s.bitb;
        int k = s.bitk;
        int q = s.write;
        int m = q < s.read ? s.read - q - 1 : s.end - q;
        block12 : do {
            switch (this.mode) {
                int e;
                int tindex;
                int j;
                case 0: {
                    if (m >= 258 && n >= 10) {
                        s.bitb = b;
                        s.bitk = k;
                        z.avail_in = n;
                        z.total_in += (long)(p - z.next_in_index);
                        z.next_in_index = p;
                        s.write = q;
                        r = InfCodes.inflate_fast(this.lbits, this.dbits, this.ltree, this.ltree_index, this.dtree, this.dtree_index, s, z);
                        p = z.next_in_index;
                        n = z.avail_in;
                        b = s.bitb;
                        k = s.bitk;
                        q = s.write;
                        int n2 = m = q < s.read ? s.read - q - 1 : s.end - q;
                        if (r != 0) {
                            this.mode = r == 1 ? 7 : 9;
                            continue block12;
                        }
                    }
                    this.need = this.lbits;
                    this.tree = this.ltree;
                    this.tree_index = this.ltree_index;
                    this.mode = 1;
                }
                case 1: {
                    j = this.need;
                    while (k < j) {
                        if (n == 0) {
                            s.bitb = b;
                            s.bitk = k;
                            z.avail_in = n;
                            z.total_in += (long)(p - z.next_in_index);
                            z.next_in_index = p;
                            s.write = q;
                            return s.inflate_flush(z, r);
                        }
                        r = 0;
                        --n;
                        b |= (z.next_in[p++] & 255) << k;
                        k += 8;
                    }
                    tindex = (this.tree_index + (b & inflate_mask[j])) * 3;
                    b >>>= this.tree[tindex + 1];
                    k -= this.tree[tindex + 1];
                    e = this.tree[tindex];
                    if (e == 0) {
                        this.lit = this.tree[tindex + 2];
                        this.mode = 6;
                        continue block12;
                    }
                    if ((e & 16) != 0) {
                        this.get = e & 15;
                        this.len = this.tree[tindex + 2];
                        this.mode = 2;
                        continue block12;
                    }
                    if ((e & 64) == 0) {
                        this.need = e;
                        this.tree_index = tindex / 3 + this.tree[tindex + 2];
                        continue block12;
                    }
                    if ((e & 32) != 0) {
                        this.mode = 7;
                        continue block12;
                    }
                    this.mode = 9;
                    z.msg = "invalid literal/length code";
                    r = -3;
                    s.bitb = b;
                    s.bitk = k;
                    z.avail_in = n;
                    z.total_in += (long)(p - z.next_in_index);
                    z.next_in_index = p;
                    s.write = q;
                    return s.inflate_flush(z, r);
                }
                case 2: {
                    j = this.get;
                    while (k < j) {
                        if (n == 0) {
                            s.bitb = b;
                            s.bitk = k;
                            z.avail_in = n;
                            z.total_in += (long)(p - z.next_in_index);
                            z.next_in_index = p;
                            s.write = q;
                            return s.inflate_flush(z, r);
                        }
                        r = 0;
                        --n;
                        b |= (z.next_in[p++] & 255) << k;
                        k += 8;
                    }
                    this.len += b & inflate_mask[j];
                    b >>= j;
                    k -= j;
                    this.need = this.dbits;
                    this.tree = this.dtree;
                    this.tree_index = this.dtree_index;
                    this.mode = 3;
                }
                case 3: {
                    j = this.need;
                    while (k < j) {
                        if (n == 0) {
                            s.bitb = b;
                            s.bitk = k;
                            z.avail_in = n;
                            z.total_in += (long)(p - z.next_in_index);
                            z.next_in_index = p;
                            s.write = q;
                            return s.inflate_flush(z, r);
                        }
                        r = 0;
                        --n;
                        b |= (z.next_in[p++] & 255) << k;
                        k += 8;
                    }
                    tindex = (this.tree_index + (b & inflate_mask[j])) * 3;
                    b >>= this.tree[tindex + 1];
                    k -= this.tree[tindex + 1];
                    e = this.tree[tindex];
                    if ((e & 16) != 0) {
                        this.get = e & 15;
                        this.dist = this.tree[tindex + 2];
                        this.mode = 4;
                        continue block12;
                    }
                    if ((e & 64) == 0) {
                        this.need = e;
                        this.tree_index = tindex / 3 + this.tree[tindex + 2];
                        continue block12;
                    }
                    this.mode = 9;
                    z.msg = "invalid distance code";
                    r = -3;
                    s.bitb = b;
                    s.bitk = k;
                    z.avail_in = n;
                    z.total_in += (long)(p - z.next_in_index);
                    z.next_in_index = p;
                    s.write = q;
                    return s.inflate_flush(z, r);
                }
                case 4: {
                    j = this.get;
                    while (k < j) {
                        if (n == 0) {
                            s.bitb = b;
                            s.bitk = k;
                            z.avail_in = n;
                            z.total_in += (long)(p - z.next_in_index);
                            z.next_in_index = p;
                            s.write = q;
                            return s.inflate_flush(z, r);
                        }
                        r = 0;
                        --n;
                        b |= (z.next_in[p++] & 255) << k;
                        k += 8;
                    }
                    this.dist += b & inflate_mask[j];
                    b >>= j;
                    k -= j;
                    this.mode = 5;
                }
                case 5: {
                    int f;
                    for (f = q - this.dist; f < 0; f += s.end) {
                    }
                    while (this.len != 0) {
                        if (m == 0) {
                            if (q == s.end && s.read != 0) {
                                q = 0;
                                int n3 = m = q < s.read ? s.read - q - 1 : s.end - q;
                            }
                            if (m == 0) {
                                s.write = q;
                                r = s.inflate_flush(z, r);
                                q = s.write;
                                int n4 = m = q < s.read ? s.read - q - 1 : s.end - q;
                                if (q == s.end && s.read != 0) {
                                    q = 0;
                                    int n5 = m = q < s.read ? s.read - q - 1 : s.end - q;
                                }
                                if (m == 0) {
                                    s.bitb = b;
                                    s.bitk = k;
                                    z.avail_in = n;
                                    z.total_in += (long)(p - z.next_in_index);
                                    z.next_in_index = p;
                                    s.write = q;
                                    return s.inflate_flush(z, r);
                                }
                            }
                        }
                        s.window[q++] = s.window[f++];
                        --m;
                        if (f == s.end) {
                            f = 0;
                        }
                        --this.len;
                    }
                    this.mode = 0;
                    continue block12;
                }
                case 6: {
                    if (m == 0) {
                        if (q == s.end && s.read != 0) {
                            q = 0;
                            int n6 = m = q < s.read ? s.read - q - 1 : s.end - q;
                        }
                        if (m == 0) {
                            s.write = q;
                            r = s.inflate_flush(z, r);
                            q = s.write;
                            int n7 = m = q < s.read ? s.read - q - 1 : s.end - q;
                            if (q == s.end && s.read != 0) {
                                q = 0;
                                int n8 = m = q < s.read ? s.read - q - 1 : s.end - q;
                            }
                            if (m == 0) {
                                s.bitb = b;
                                s.bitk = k;
                                z.avail_in = n;
                                z.total_in += (long)(p - z.next_in_index);
                                z.next_in_index = p;
                                s.write = q;
                                return s.inflate_flush(z, r);
                            }
                        }
                    }
                    r = 0;
                    s.window[q++] = (byte)this.lit;
                    --m;
                    this.mode = 0;
                    continue block12;
                }
                case 7: {
                    if (k > 7) {
                        k -= 8;
                        ++n;
                        --p;
                    }
                    s.write = q;
                    r = s.inflate_flush(z, r);
                    q = s.write;
                    if (s.read != s.write) {
                        s.bitb = b;
                        s.bitk = k;
                        z.avail_in = n;
                        z.total_in += (long)(p - z.next_in_index);
                        z.next_in_index = p;
                        s.write = q;
                        return s.inflate_flush(z, r);
                    }
                    this.mode = 8;
                }
                case 8: {
                    r = 1;
                    s.bitb = b;
                    s.bitk = k;
                    z.avail_in = n;
                    z.total_in += (long)(p - z.next_in_index);
                    z.next_in_index = p;
                    s.write = q;
                    return s.inflate_flush(z, r);
                }
                case 9: {
                    r = -3;
                    s.bitb = b;
                    s.bitk = k;
                    z.avail_in = n;
                    z.total_in += (long)(p - z.next_in_index);
                    z.next_in_index = p;
                    s.write = q;
                    return s.inflate_flush(z, r);
                }
            }
            break;
        } while (true);
        r = -2;
        s.bitb = b;
        s.bitk = k;
        z.avail_in = n;
        z.total_in += (long)(p - z.next_in_index);
        z.next_in_index = p;
        s.write = q;
        return s.inflate_flush(z, r);
    }

    /*
     * Unable to fully structure code
     * Enabled aggressive block sorting
     * Lifted jumps to return sites
     */
    static int inflate_fast(int bl, int bd, int[] tl, int tl_index, int[] td, int td_index, InfBlocks s, ZStream z) {
        p = z.next_in_index;
        n = z.avail_in;
        b = s.bitb;
        k = s.bitk;
        q = s.write;
        m = q < s.read ? s.read - q - 1 : s.end - q;
        ml = InfCodes.inflate_mask[bl];
        md = InfCodes.inflate_mask[bd];
        do {
            block25 : {
                block23 : {
                    block24 : {
                        if (k < 20) {
                            --n;
                            b |= (z.next_in[p++] & 255) << k;
                            k += 8;
                            continue;
                        }
                        tp = tl;
                        tp_index = tl_index;
                        t = b & ml;
                        tp_index_t_3 = (tp_index + t) * 3;
                        e = tp[tp_index_t_3];
                        if (e == 0) {
                            b >>= tp[tp_index_t_3 + 1];
                            k -= tp[tp_index_t_3 + 1];
                            s.window[q++] = (byte)tp[tp_index_t_3 + 2];
                            --m;
                        } else {
                            do {
                                b >>= tp[tp_index_t_3 + 1];
                                k -= tp[tp_index_t_3 + 1];
                                if ((e & 16) != 0) {
                                    c = tp[tp_index_t_3 + 2] + (b & InfCodes.inflate_mask[e &= 15]);
                                    b >>= e;
                                    k -= e;
                                    while (k < 15) {
                                        --n;
                                        b |= (z.next_in[p++] & 255) << k;
                                        k += 8;
                                    }
                                    break block23;
                                }
                                if ((e & 64) != 0) break block24;
                                t += tp[tp_index_t_3 + 2];
                            } while ((e = tp[tp_index_t_3 = (tp_index + (t += b & InfCodes.inflate_mask[e])) * 3]) != 0);
                            b >>= tp[tp_index_t_3 + 1];
                            k -= tp[tp_index_t_3 + 1];
                            s.window[q++] = (byte)tp[tp_index_t_3 + 2];
                            --m;
                        }
                        ** GOTO lbl138
                    }
                    if ((e & 32) != 0) {
                        c = z.avail_in - n;
                        c = k >> 3 < c ? k >> 3 : c;
                        s.bitb = b;
                        s.bitk = k -= c << 3;
                        z.avail_in = n += c;
                        z.total_in += (long)((p -= c) - z.next_in_index);
                        z.next_in_index = p;
                        s.write = q;
                        return 1;
                    }
                    z.msg = "invalid literal/length code";
                    c = z.avail_in - n;
                    c = k >> 3 < c ? k >> 3 : c;
                    s.bitb = b;
                    s.bitk = k -= c << 3;
                    z.avail_in = n += c;
                    z.total_in += (long)((p -= c) - z.next_in_index);
                    z.next_in_index = p;
                    s.write = q;
                    return -3;
                }
                t = b & md;
                tp = td;
                tp_index = td_index;
                tp_index_t_3 = (tp_index + t) * 3;
                e = tp[tp_index_t_3];
                do {
                    b >>= tp[tp_index_t_3 + 1];
                    k -= tp[tp_index_t_3 + 1];
                    if ((e & 16) != 0) {
                        while (k < (e &= 15)) {
                            --n;
                            b |= (z.next_in[p++] & 255) << k;
                            k += 8;
                        }
                        d = tp[tp_index_t_3 + 2] + (b & InfCodes.inflate_mask[e]);
                        b >>= e;
                        k -= e;
                        m -= c;
                        if (q >= d) {
                            r = q - d;
                            if (q - r > 0 && 2 > q - r) {
                                s.window[q++] = s.window[r++];
                                s.window[q++] = s.window[r++];
                                c -= 2;
                            } else {
                                System.arraycopy(s.window, r, s.window, q, 2);
                                q += 2;
                                r += 2;
                                c -= 2;
                            }
                        } else {
                            r = q - d;
                            while ((r += s.end) < 0) {
                            }
                            e = s.end - r;
                            if (c > e) {
                                c -= e;
                                if (q - r > 0 && e > q - r) {
                                    do {
                                        s.window[q++] = s.window[r++];
                                    } while (--e != 0);
                                } else {
                                    System.arraycopy(s.window, r, s.window, q, e);
                                    q += e;
                                    r += e;
                                }
                                r = 0;
                            }
                        }
                        if (q - r > 0 && c > q - r) {
                            do {
                                s.window[q++] = s.window[r++];
                            } while (--c != 0);
                        } else {
                            System.arraycopy(s.window, r, s.window, q, c);
                            q += c;
                            r += c;
                        }
                        break block25;
                    }
                    if ((e & 64) != 0) break;
                    t += tp[tp_index_t_3 + 2];
                    tp_index_t_3 = (tp_index + (t += b & InfCodes.inflate_mask[e])) * 3;
                    e = tp[tp_index_t_3];
                } while (true);
                z.msg = "invalid distance code";
                c = z.avail_in - n;
                c = k >> 3 < c ? k >> 3 : c;
                s.bitb = b;
                s.bitk = k -= c << 3;
                z.avail_in = n += c;
                z.total_in += (long)((p -= c) - z.next_in_index);
                z.next_in_index = p;
                s.write = q;
                return -3;
            }
            if (m < 258 || n < 10) break;
        } while (true);
        c = z.avail_in - n;
        c = k >> 3 < c ? k >> 3 : c;
        s.bitb = b;
        s.bitk = k -= c << 3;
        z.avail_in = n += c;
        z.total_in += (long)((p -= c) - z.next_in_index);
        z.next_in_index = p;
        s.write = q;
        return 0;
    }
}

