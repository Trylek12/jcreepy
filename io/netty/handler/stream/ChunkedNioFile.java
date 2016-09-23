
package io.netty.handler.stream;

import io.netty.buffer.ByteBuf;
import io.netty.handler.stream.ChunkedByteInput;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.ScatteringByteChannel;

public class ChunkedNioFile
implements ChunkedByteInput {
    private final FileChannel in;
    private final long startOffset;
    private final long endOffset;
    private final int chunkSize;
    private long offset;

    public ChunkedNioFile(File in) throws IOException {
        this(new FileInputStream(in).getChannel());
    }

    public ChunkedNioFile(File in, int chunkSize) throws IOException {
        this(new FileInputStream(in).getChannel(), chunkSize);
    }

    public ChunkedNioFile(FileChannel in) throws IOException {
        this(in, 8192);
    }

    public ChunkedNioFile(FileChannel in, int chunkSize) throws IOException {
        this(in, 0, in.size(), chunkSize);
    }

    public ChunkedNioFile(FileChannel in, long offset, long length, int chunkSize) throws IOException {
        if (in == null) {
            throw new NullPointerException("in");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset: " + offset + " (expected: 0 or greater)");
        }
        if (length < 0) {
            throw new IllegalArgumentException("length: " + length + " (expected: 0 or greater)");
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize: " + chunkSize + " (expected: a positive integer)");
        }
        if (offset != 0) {
            in.position(offset);
        }
        this.in = in;
        this.chunkSize = chunkSize;
        this.offset = this.startOffset = offset;
        this.endOffset = offset + length;
    }

    public long getStartOffset() {
        return this.startOffset;
    }

    public long getEndOffset() {
        return this.endOffset;
    }

    public long getCurrentOffset() {
        return this.offset;
    }

    @Override
    public boolean isEndOfInput() throws Exception {
        return this.offset >= this.endOffset || !this.in.isOpen();
    }

    @Override
    public void close() throws Exception {
        this.in.close();
    }

    @Override
    public boolean readChunk(ByteBuf buffer) throws Exception {
        int localReadBytes;
        long offset = this.offset;
        if (offset >= this.endOffset) {
            return false;
        }
        int chunkSize = (int)Math.min((long)this.chunkSize, this.endOffset - offset);
        int readBytes = 0;
        while ((localReadBytes = buffer.writeBytes(this.in, chunkSize - readBytes)) >= 0 && (readBytes += localReadBytes) != chunkSize) {
        }
        this.offset += (long)readBytes;
        return true;
    }
}

