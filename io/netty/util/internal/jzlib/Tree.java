
package io.netty.util.internal.jzlib;

import io.netty.util.internal.jzlib.Deflate;
import io.netty.util.internal.jzlib.StaticTree;

final class Tree {
    static final int[] extra_lbits = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0};
    static final int[] extra_dbits = new int[]{0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13};
    static final int[] extra_blbits = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 3, 7};
    static final byte[] bl_order = new byte[]{16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15};
    static final byte[] _dist_code = new byte[]{0, 1, 2, 3, 4, 4, 5, 5, 6, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8, 8, 8, 8, 8, 8, 9, 9, 9, 9, 9, 9, 9, 9, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 0, 0, 16, 17, 18, 18, 19, 19, 20, 20, 20, 20, 21, 21, 21, 21, 22, 22, 22, 22, 22, 22, 22, 22, 23, 23, 23, 23, 23, 23, 23, 23, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29};
    static final byte[] _length_code = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 12, 12, 13, 13, 13, 13, 14, 14, 14, 14, 15, 15, 15, 15, 16, 16, 16, 16, 16, 16, 16, 16, 17, 17, 17, 17, 17, 17, 17, 17, 18, 18, 18, 18, 18, 18, 18, 18, 19, 19, 19, 19, 19, 19, 19, 19, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 28};
    static final int[] base_length = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 12, 14, 16, 20, 24, 28, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 0};
    static final int[] base_dist = new int[]{0, 1, 2, 3, 4, 6, 8, 12, 16, 24, 32, 48, 64, 96, 128, 192, 256, 384, 512, 768, 1024, 1536, 2048, 3072, 4096, 6144, 8192, 12288, 16384, 24576};
    short[] dyn_tree;
    int max_code;
    StaticTree stat_desc;

    Tree() {
    }

    static int d_code(int dist) {
        return dist < 256 ? _dist_code[dist] : _dist_code[256 + (dist >>> 7)];
    }

    private void gen_bitlen(Deflate s) {
        int n;
        int bits;
        int h;
        short[] tree = this.dyn_tree;
        short[] stree = this.stat_desc.static_tree;
        int[] extra = this.stat_desc.extra_bits;
        int base = this.stat_desc.extra_base;
        int max_length = this.stat_desc.max_length;
        int overflow = 0;
        for (bits = 0; bits <= 15; ++bits) {
            s.bl_count[bits] = 0;
        }
        tree[s.heap[s.heap_max] * 2 + 1] = 0;
        for (h = s.heap_max + 1; h < 573; ++h) {
            n = s.heap[h];
            bits = tree[tree[n * 2 + 1] * 2 + 1] + 1;
            if (bits > max_length) {
                bits = max_length;
                ++overflow;
            }
            tree[n * 2 + 1] = (short)bits;
            if (n > this.max_code) continue;
            short[] arrs = s.bl_count;
            int n2 = bits;
            arrs[n2] = (short)(arrs[n2] + 1);
            int xbits = 0;
            if (n >= base) {
                xbits = extra[n - base];
            }
            short f = tree[n * 2];
            s.opt_len += f * (bits + xbits);
            if (stree == null) continue;
            s.static_len += f * (stree[n * 2 + 1] + xbits);
        }
        if (overflow == 0) {
            return;
        }
        do {
            bits = max_length - 1;
            while (s.bl_count[bits] == 0) {
                --bits;
            }
            short[] arrs = s.bl_count;
            int n3 = bits;
            arrs[n3] = (short)(arrs[n3] - 1);
            short[] arrs2 = s.bl_count;
            int n4 = bits + 1;
            arrs2[n4] = (short)(arrs2[n4] + 2);
            short[] arrs3 = s.bl_count;
            int n5 = max_length;
            arrs3[n5] = (short)(arrs3[n5] - 1);
        } while ((overflow -= 2) > 0);
        for (bits = max_length; bits != 0; --bits) {
            n = s.bl_count[bits];
            while (n != 0) {
                int m;
                if ((m = s.heap[--h]) > this.max_code) continue;
                if (tree[m * 2 + 1] != bits) {
                    s.opt_len = (int)((long)s.opt_len + ((long)bits - (long)tree[m * 2 + 1]) * (long)tree[m * 2]);
                    tree[m * 2 + 1] = (short)bits;
                }
                --n;
            }
        }
    }

    void build_tree(Deflate s) {
        int node;
        int n;
        short[] tree = this.dyn_tree;
        short[] stree = this.stat_desc.static_tree;
        int elems = this.stat_desc.elems;
        int max_code = -1;
        s.heap_len = 0;
        s.heap_max = 573;
        for (n = 0; n < elems; ++n) {
            if (tree[n * 2] != 0) {
                s.heap[++s.heap_len] = max_code = n;
                s.depth[n] = 0;
                continue;
            }
            tree[n * 2 + 1] = 0;
        }
        while (s.heap_len < 2) {
            int n2 = max_code < 2 ? ++max_code : 0;
            s.heap[++s.heap_len] = n2;
            node = n2;
            tree[node * 2] = 1;
            s.depth[node] = 0;
            --s.opt_len;
            if (stree == null) continue;
            s.static_len -= stree[node * 2 + 1];
        }
        this.max_code = max_code;
        for (n = s.heap_len / 2; n >= 1; --n) {
            s.pqdownheap(tree, n);
        }
        node = elems;
        do {
            n = s.heap[1];
            s.heap[1] = s.heap[s.heap_len--];
            s.pqdownheap(tree, 1);
            int m = s.heap[1];
            s.heap[--s.heap_max] = n;
            s.heap[--s.heap_max] = m;
            tree[node * 2] = (short)(tree[n * 2] + tree[m * 2]);
            s.depth[node] = (byte)(Math.max(s.depth[n], s.depth[m]) + 1);
            short s2 = (short)node;
            tree[m * 2 + 1] = s2;
            tree[n * 2 + 1] = s2;
            s.heap[1] = node++;
            s.pqdownheap(tree, 1);
        } while (s.heap_len >= 2);
        s.heap[--s.heap_max] = s.heap[1];
        this.gen_bitlen(s);
        Tree.gen_codes(tree, max_code, s.bl_count);
    }

    private static void gen_codes(short[] tree, int max_code, short[] bl_count) {
        short[] next_code = new short[16];
        short code = 0;
        for (int bits = 1; bits <= 15; ++bits) {
            next_code[bits] = code = (short)(code + bl_count[bits - 1] << 1);
        }
        for (int n = 0; n <= max_code; ++n) {
            short len = tree[n * 2 + 1];
            if (len == 0) continue;
            short[] arrs = next_code;
            short s = len;
            short s2 = arrs[s];
            arrs[s] = (short)(s2 + 1);
            tree[n * 2] = (short)Tree.bi_reverse(s2, len);
        }
    }

    private static int bi_reverse(int code, int len) {
        int res = 0;
        do {
            res |= code & 1;
            code >>>= 1;
            res <<= 1;
        } while (--len > 0);
        return res >>> 1;
    }
}

