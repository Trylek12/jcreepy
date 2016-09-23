
package io.netty.handler.stream;

import io.netty.buffer.ByteBuf;
import io.netty.handler.stream.ChunkedByteInput;
import java.io.InputStream;
import java.io.PushbackInputStream;

public class ChunkedStream
implements ChunkedByteInput {
    static final int DEFAULT_CHUNK_SIZE = 8192;
    private final PushbackInputStream in;
    private final int chunkSize;
    private long offset;

    public ChunkedStream(InputStream in) {
        this(in, 8192);
    }

    public ChunkedStream(InputStream in, int chunkSize) {
        if (in == null) {
            throw new NullPointerException("in");
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize: " + chunkSize + " (expected: a positive integer)");
        }
        this.in = in instanceof PushbackInputStream ? (PushbackInputStream)in : new PushbackInputStream(in);
        this.chunkSize = chunkSize;
    }

    public long getTransferredBytes() {
        return this.offset;
    }

    @Override
    public boolean isEndOfInput() throws Exception {
        int b = this.in.read();
        if (b < 0) {
            return true;
        }
        this.in.unread(b);
        return false;
    }

    @Override
    public void close() throws Exception {
        this.in.close();
    }

    @Override
    public boolean readChunk(ByteBuf buffer) throws Exception {
        if (this.isEndOfInput()) {
            return false;
        }
        int availableBytes = this.in.available();
        int chunkSize = availableBytes <= 0 ? this.chunkSize : Math.min(this.chunkSize, this.in.available());
        this.offset += (long)buffer.writeBytes(this.in, chunkSize);
        return true;
    }
}

