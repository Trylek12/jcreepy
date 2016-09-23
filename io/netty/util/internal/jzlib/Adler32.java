
package io.netty.util.internal.jzlib;

final class Adler32 {
    private static final int BASE = 65521;
    private static final int NMAX = 5552;

    static long adler32(long adler, byte[] buf, int index, int len) {
        if (buf == null) {
            return 1;
        }
        long s1 = adler & 65535;
        long s2 = adler >> 16 & 65535;
        while (len > 0) {
            int k = len < 5552 ? len : 5552;
            len -= k;
            while (k >= 16) {
                s2 += (s1 += (long)(buf[index++] & 255));
                s2 += (s1 += (long)(buf[index++] & 255));
                s2 += (s1 += (long)(buf[index++] & 255));
                s2 += (s1 += (long)(buf[index++] & 255));
                s2 += (s1 += (long)(buf[index++] & 255));
                s2 += (s1 += (long)(buf[index++] & 255));
                s2 += (s1 += (long)(buf[index++] & 255));
                s2 += (s1 += (long)(buf[index++] & 255));
                s2 += (s1 += (long)(buf[index++] & 255));
                s2 += (s1 += (long)(buf[index++] & 255));
                s2 += (s1 += (long)(buf[index++] & 255));
                s2 += (s1 += (long)(buf[index++] & 255));
                s2 += (s1 += (long)(buf[index++] & 255));
                s2 += (s1 += (long)(buf[index++] & 255));
                s2 += (s1 += (long)(buf[index++] & 255));
                s2 += (s1 += (long)(buf[index++] & 255));
                k -= 16;
            }
            if (k != 0) {
                do {
                    s2 += (s1 += (long)(buf[index++] & 255));
                } while (--k != 0);
            }
            s1 %= 65521;
            s2 %= 65521;
        }
        return s2 << 16 | s1;
    }

    private Adler32() {
    }
}

