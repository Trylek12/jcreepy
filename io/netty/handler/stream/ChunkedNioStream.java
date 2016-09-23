
package io.netty.handler.stream;

import io.netty.buffer.ByteBuf;
import io.netty.handler.stream.ChunkedByteInput;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class ChunkedNioStream
implements ChunkedByteInput {
    private final ReadableByteChannel in;
    private final int chunkSize;
    private long offset;
    private final ByteBuffer byteBuffer;

    public ChunkedNioStream(ReadableByteChannel in) {
        this(in, 8192);
    }

    public ChunkedNioStream(ReadableByteChannel in, int chunkSize) {
        if (in == null) {
            throw new NullPointerException("in");
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize: " + chunkSize + " (expected: a positive integer)");
        }
        this.in = in;
        this.offset = 0;
        this.chunkSize = chunkSize;
        this.byteBuffer = ByteBuffer.allocate(chunkSize);
    }

    public long getTransferredBytes() {
        return this.offset;
    }

    @Override
    public boolean isEndOfInput() throws Exception {
        if (this.byteBuffer.position() > 0) {
            return false;
        }
        if (this.in.isOpen()) {
            int b = this.in.read(this.byteBuffer);
            if (b < 0) {
                return true;
            }
            this.offset += (long)b;
            return false;
        }
        return true;
    }

    @Override
    public void close() throws Exception {
        this.in.close();
    }

    @Override
    public boolean readChunk(ByteBuf buffer) throws Exception {
        int localReadBytes;
        if (this.isEndOfInput()) {
            return false;
        }
        int readBytes = this.byteBuffer.position();
        while ((localReadBytes = this.in.read(this.byteBuffer)) >= 0) {
            this.offset += (long)localReadBytes;
            if ((readBytes += localReadBytes) != this.chunkSize) continue;
            break;
        }
        this.byteBuffer.flip();
        buffer.writeBytes(this.byteBuffer);
        this.byteBuffer.clear();
        return true;
    }
}

