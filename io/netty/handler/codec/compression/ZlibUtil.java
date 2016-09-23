
package io.netty.handler.codec.compression;

import io.netty.handler.codec.compression.CompressionException;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.util.internal.jzlib.JZlib;
import io.netty.util.internal.jzlib.ZStream;

final class ZlibUtil {
    static void fail(ZStream z, String message, int resultCode) {
        throw ZlibUtil.exception(z, message, resultCode);
    }

    static CompressionException exception(ZStream z, String message, int resultCode) {
        return new CompressionException(message + " (" + resultCode + ')' + (z.msg != null ? new StringBuilder().append(": ").append(z.msg).toString() : ""));
    }

    static Enum<?> convertWrapperType(ZlibWrapper wrapper) {
        Enum convertedWrapperType;
        switch (wrapper) {
            case NONE: {
                convertedWrapperType = JZlib.W_NONE;
                break;
            }
            case ZLIB: {
                convertedWrapperType = JZlib.W_ZLIB;
                break;
            }
            case GZIP: {
                convertedWrapperType = JZlib.W_GZIP;
                break;
            }
            case ZLIB_OR_NONE: {
                convertedWrapperType = JZlib.W_ZLIB_OR_NONE;
                break;
            }
            default: {
                throw new Error();
            }
        }
        return convertedWrapperType;
    }

    private ZlibUtil() {
    }

}

