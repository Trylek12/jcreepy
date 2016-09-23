
package io.netty.handler.codec.compression;

import io.netty.handler.codec.compression.JZlibDecoder;
import io.netty.handler.codec.compression.JZlibEncoder;
import io.netty.handler.codec.compression.JdkZlibEncoder;
import io.netty.handler.codec.compression.ZlibDecoder;
import io.netty.handler.codec.compression.ZlibEncoder;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.util.internal.DetectionUtil;

public final class ZlibCodecFactory {
    public static ZlibEncoder newZlibEncoder(int compressionLevel) {
        if (DetectionUtil.javaVersion() < 7) {
            return new JZlibEncoder(compressionLevel);
        }
        return new JdkZlibEncoder(compressionLevel);
    }

    public static ZlibEncoder newZlibEncoder(ZlibWrapper wrapper) {
        if (DetectionUtil.javaVersion() < 7) {
            return new JZlibEncoder(wrapper);
        }
        return new JdkZlibEncoder(wrapper);
    }

    public static ZlibEncoder newZlibEncoder(ZlibWrapper wrapper, int compressionLevel) {
        if (DetectionUtil.javaVersion() < 7) {
            return new JZlibEncoder(wrapper, compressionLevel);
        }
        return new JdkZlibEncoder(wrapper, compressionLevel);
    }

    public static ZlibEncoder newZlibEncoder(ZlibWrapper wrapper, int compressionLevel, int windowBits, int memLevel) {
        if (DetectionUtil.javaVersion() < 7) {
            return new JZlibEncoder(wrapper, compressionLevel, windowBits, memLevel);
        }
        return new JdkZlibEncoder(wrapper, compressionLevel);
    }

    public static ZlibEncoder newZlibEncoder(byte[] dictionary) {
        if (DetectionUtil.javaVersion() < 7) {
            return new JZlibEncoder(dictionary);
        }
        return new JdkZlibEncoder(dictionary);
    }

    public static ZlibEncoder newZlibEncoder(int compressionLevel, byte[] dictionary) {
        if (DetectionUtil.javaVersion() < 7) {
            return new JZlibEncoder(compressionLevel, dictionary);
        }
        return new JdkZlibEncoder(compressionLevel, dictionary);
    }

    public static ZlibEncoder newZlibEncoder(int compressionLevel, int windowBits, int memLevel, byte[] dictionary) {
        if (DetectionUtil.javaVersion() < 7) {
            return new JZlibEncoder(compressionLevel, windowBits, memLevel, dictionary);
        }
        return new JdkZlibEncoder(compressionLevel, dictionary);
    }

    public static ZlibDecoder newZlibDecoder() {
        return new JZlibDecoder();
    }

    public static ZlibDecoder newZlibDecoder(ZlibWrapper wrapper) {
        return new JZlibDecoder(wrapper);
    }

    public static ZlibDecoder newZlibDecoder(byte[] dictionary) {
        return new JZlibDecoder(dictionary);
    }

    private ZlibCodecFactory() {
    }
}

