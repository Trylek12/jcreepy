
package io.netty.handler.stream;

import io.netty.buffer.ByteBuf;
import io.netty.handler.stream.ChunkedByteInput;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

public class ChunkedFile
implements ChunkedByteInput {
    private final RandomAccessFile file;
    private final long startOffset;
    private final long endOffset;
    private final int chunkSize;
    private long offset;

    public ChunkedFile(File file) throws IOException {
        this(file, 8192);
    }

    public ChunkedFile(File file, int chunkSize) throws IOException {
        this(new RandomAccessFile(file, "r"), chunkSize);
    }

    public ChunkedFile(RandomAccessFile file) throws IOException {
        this(file, 8192);
    }

    public ChunkedFile(RandomAccessFile file, int chunkSize) throws IOException {
        this(file, 0, file.length(), chunkSize);
    }

    public ChunkedFile(RandomAccessFile file, long offset, long length, int chunkSize) throws IOException {
        if (file == null) {
            throw new NullPointerException("file");
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
        this.file = file;
        this.offset = this.startOffset = offset;
        this.endOffset = offset + length;
        this.chunkSize = chunkSize;
        file.seek(offset);
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
        return this.offset >= this.endOffset || !this.file.getChannel().isOpen();
    }

    @Override
    public void close() throws Exception {
        this.file.close();
    }

    @Override
    public boolean readChunk(ByteBuf buffer) throws Exception {
        long offset = this.offset;
        if (offset >= this.endOffset) {
            return false;
        }
        int chunkSize = (int)Math.min((long)this.chunkSize, this.endOffset - offset);
        byte[] chunk = new byte[chunkSize];
        this.file.readFully(chunk);
        buffer.writeBytes(chunk);
        this.offset = offset + (long)chunkSize;
        return true;
    }
}

