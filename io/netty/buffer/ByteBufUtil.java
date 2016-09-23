
package io.netty.buffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufIndexFinder;
import io.netty.util.CharsetUtil;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

public final class ByteBufUtil {
    private static final char[] HEXDUMP_TABLE = new char[1024];

    public static String hexDump(ByteBuf buffer) {
        return ByteBufUtil.hexDump(buffer, buffer.readerIndex(), buffer.readableBytes());
    }

    public static String hexDump(ByteBuf buffer, int fromIndex, int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length: " + length);
        }
        if (length == 0) {
            return "";
        }
        int endIndex = fromIndex + length;
        char[] buf = new char[length << 1];
        int srcIdx = fromIndex;
        int dstIdx = 0;
        while (srcIdx < endIndex) {
            System.arraycopy(HEXDUMP_TABLE, buffer.getUnsignedByte(srcIdx) << 1, buf, dstIdx, 2);
            ++srcIdx;
            dstIdx += 2;
        }
        return new String(buf);
    }

    public static int hashCode(ByteBuf buffer) {
        int i;
        int aLen = buffer.readableBytes();
        int intCount = aLen >>> 2;
        int byteCount = aLen & 3;
        int hashCode = 1;
        int arrayIndex = buffer.readerIndex();
        if (buffer.order() == ByteOrder.BIG_ENDIAN) {
            for (i = intCount; i > 0; --i) {
                hashCode = 31 * hashCode + buffer.getInt(arrayIndex);
                arrayIndex += 4;
            }
        } else {
            for (i = intCount; i > 0; --i) {
                hashCode = 31 * hashCode + ByteBufUtil.swapInt(buffer.getInt(arrayIndex));
                arrayIndex += 4;
            }
        }
        for (i = byteCount; i > 0; --i) {
            hashCode = 31 * hashCode + buffer.getByte(arrayIndex++);
        }
        if (hashCode == 0) {
            hashCode = 1;
        }
        return hashCode;
    }

    public static boolean equals(ByteBuf bufferA, ByteBuf bufferB) {
        int i;
        int aLen = bufferA.readableBytes();
        if (aLen != bufferB.readableBytes()) {
            return false;
        }
        int longCount = aLen >>> 3;
        int byteCount = aLen & 7;
        int aIndex = bufferA.readerIndex();
        int bIndex = bufferB.readerIndex();
        if (bufferA.order() == bufferB.order()) {
            for (i = longCount; i > 0; --i) {
                if (bufferA.getLong(aIndex) != bufferB.getLong(bIndex)) {
                    return false;
                }
                aIndex += 8;
                bIndex += 8;
            }
        } else {
            for (i = longCount; i > 0; --i) {
                if (bufferA.getLong(aIndex) != ByteBufUtil.swapLong(bufferB.getLong(bIndex))) {
                    return false;
                }
                aIndex += 8;
                bIndex += 8;
            }
        }
        for (i = byteCount; i > 0; --i) {
            if (bufferA.getByte(aIndex) != bufferB.getByte(bIndex)) {
                return false;
            }
            ++aIndex;
            ++bIndex;
        }
        return true;
    }

    public static int compare(ByteBuf bufferA, ByteBuf bufferB) {
        long va;
        int i;
        long vb;
        int aLen = bufferA.readableBytes();
        int bLen = bufferB.readableBytes();
        int minLength = Math.min(aLen, bLen);
        int uintCount = minLength >>> 2;
        int byteCount = minLength & 3;
        int aIndex = bufferA.readerIndex();
        int bIndex = bufferB.readerIndex();
        if (bufferA.order() == bufferB.order()) {
            for (i = uintCount; i > 0; --i) {
                va = bufferA.getUnsignedInt(aIndex);
                if (va > (vb = bufferB.getUnsignedInt(bIndex))) {
                    return 1;
                }
                if (va < vb) {
                    return -1;
                }
                aIndex += 4;
                bIndex += 4;
            }
        } else {
            for (i = uintCount; i > 0; --i) {
                va = bufferA.getUnsignedInt(aIndex);
                if (va > (vb = (long)ByteBufUtil.swapInt(bufferB.getInt(bIndex)) & 0xFFFFFFFFL)) {
                    return 1;
                }
                if (va < vb) {
                    return -1;
                }
                aIndex += 4;
                bIndex += 4;
            }
        }
        for (i = byteCount; i > 0; --i) {
            short vb2;
            short va2 = bufferA.getUnsignedByte(aIndex);
            if (va2 > (vb2 = bufferB.getUnsignedByte(bIndex))) {
                return 1;
            }
            if (va2 < vb2) {
                return -1;
            }
            ++aIndex;
            ++bIndex;
        }
        return aLen - bLen;
    }

    public static int indexOf(ByteBuf buffer, int fromIndex, int toIndex, byte value) {
        if (fromIndex <= toIndex) {
            return ByteBufUtil.firstIndexOf(buffer, fromIndex, toIndex, value);
        }
        return ByteBufUtil.lastIndexOf(buffer, fromIndex, toIndex, value);
    }

    public static int indexOf(ByteBuf buffer, int fromIndex, int toIndex, ByteBufIndexFinder indexFinder) {
        if (fromIndex <= toIndex) {
            return ByteBufUtil.firstIndexOf(buffer, fromIndex, toIndex, indexFinder);
        }
        return ByteBufUtil.lastIndexOf(buffer, fromIndex, toIndex, indexFinder);
    }

    public static short swapShort(short value) {
        return (short)(value << 8 | value >>> 8 & 255);
    }

    public static int swapMedium(int value) {
        int swapped = value << 16 & 16711680 | value & 65280 | value >>> 16 & 255;
        if ((swapped & 8388608) != 0) {
            swapped |= -16777216;
        }
        return swapped;
    }

    public static int swapInt(int value) {
        return ByteBufUtil.swapShort((short)value) << 16 | ByteBufUtil.swapShort((short)(value >>> 16)) & 65535;
    }

    public static long swapLong(long value) {
        return (long)ByteBufUtil.swapInt((int)value) << 32 | (long)ByteBufUtil.swapInt((int)(value >>> 32)) & 0xFFFFFFFFL;
    }

    private static int firstIndexOf(ByteBuf buffer, int fromIndex, int toIndex, byte value) {
        if ((fromIndex = Math.max(fromIndex, 0)) >= toIndex || buffer.capacity() == 0) {
            return -1;
        }
        for (int i = fromIndex; i < toIndex; ++i) {
            if (buffer.getByte(i) != value) continue;
            return i;
        }
        return -1;
    }

    private static int lastIndexOf(ByteBuf buffer, int fromIndex, int toIndex, byte value) {
        if ((fromIndex = Math.min(fromIndex, buffer.capacity())) < 0 || buffer.capacity() == 0) {
            return -1;
        }
        for (int i = fromIndex - 1; i >= toIndex; --i) {
            if (buffer.getByte(i) != value) continue;
            return i;
        }
        return -1;
    }

    private static int firstIndexOf(ByteBuf buffer, int fromIndex, int toIndex, ByteBufIndexFinder indexFinder) {
        if ((fromIndex = Math.max(fromIndex, 0)) >= toIndex || buffer.capacity() == 0) {
            return -1;
        }
        for (int i = fromIndex; i < toIndex; ++i) {
            if (!indexFinder.find(buffer, i)) continue;
            return i;
        }
        return -1;
    }

    private static int lastIndexOf(ByteBuf buffer, int fromIndex, int toIndex, ByteBufIndexFinder indexFinder) {
        if ((fromIndex = Math.min(fromIndex, buffer.capacity())) < 0 || buffer.capacity() == 0) {
            return -1;
        }
        for (int i = fromIndex - 1; i >= toIndex; --i) {
            if (!indexFinder.find(buffer, i)) continue;
            return i;
        }
        return -1;
    }

    static ByteBuffer encodeString(CharBuffer src, Charset charset) {
        CharsetEncoder encoder = CharsetUtil.getEncoder(charset);
        ByteBuffer dst = ByteBuffer.allocate((int)((double)src.remaining() * (double)encoder.maxBytesPerChar()));
        try {
            CoderResult cr = encoder.encode(src, dst, true);
            if (!cr.isUnderflow()) {
                cr.throwException();
            }
            if (!(cr = encoder.flush(dst)).isUnderflow()) {
                cr.throwException();
            }
        }
        catch (CharacterCodingException x) {
            throw new IllegalStateException(x);
        }
        dst.flip();
        return dst;
    }

    static String decodeString(ByteBuffer src, Charset charset) {
        CharsetDecoder decoder = CharsetUtil.getDecoder(charset);
        CharBuffer dst = CharBuffer.allocate((int)((double)src.remaining() * (double)decoder.maxCharsPerByte()));
        try {
            CoderResult cr = decoder.decode(src, dst, true);
            if (!cr.isUnderflow()) {
                cr.throwException();
            }
            if (!(cr = decoder.flush(dst)).isUnderflow()) {
                cr.throwException();
            }
        }
        catch (CharacterCodingException x) {
            throw new IllegalStateException(x);
        }
        return dst.flip().toString();
    }

    private ByteBufUtil() {
    }

    static {
        char[] DIGITS = "0123456789abcdef".toCharArray();
        for (int i = 0; i < 256; ++i) {
            ByteBufUtil.HEXDUMP_TABLE[i << 1] = DIGITS[i >>> 4 & 15];
            ByteBufUtil.HEXDUMP_TABLE[(i << 1) + 1] = DIGITS[i & 15];
        }
    }
}

