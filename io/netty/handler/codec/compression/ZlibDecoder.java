
package io.netty.handler.codec.compression;

import io.netty.handler.codec.ByteToByteDecoder;

public abstract class ZlibDecoder
extends ByteToByteDecoder {
    public abstract boolean isClosed();
}

