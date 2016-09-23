
package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufIndexFinder;
import io.netty.buffer.ChannelBuf;
import io.netty.buffer.ChannelBufType;
import io.netty.buffer.SwappedByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.UnreplayableOperationException;
import io.netty.util.internal.Signal;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;

final class ReplayingDecoderBuffer
implements ByteBuf,
ByteBuf.Unsafe {
    private static final Signal REPLAY = ReplayingDecoder.REPLAY;
    private final ByteBuf buffer;
    private final SwappedByteBuf swapped;
    private boolean terminated;
    static final ReplayingDecoderBuffer EMPTY_BUFFER = new ReplayingDecoderBuffer(Unpooled.EMPTY_BUFFER);

    ReplayingDecoderBuffer(ByteBuf buffer) {
        this.buffer = buffer;
        this.swapped = new SwappedByteBuf(this);
    }

    void terminate() {
        this.terminated = true;
    }

    @Override
    public int capacity() {
        if (this.terminated) {
            return this.buffer.capacity();
        }
        return Integer.MAX_VALUE;
    }

    @Override
    public ByteBuf capacity(int newCapacity) {
        throw new UnreplayableOperationException();
    }

    @Override
    public int maxCapacity() {
        return this.capacity();
    }

    @Override
    public ChannelBufType type() {
        return ChannelBufType.BYTE;
    }

    @Override
    public ByteBufAllocator alloc() {
        return this.buffer.alloc();
    }

    @Override
    public boolean isDirect() {
        return this.buffer.isDirect();
    }

    @Override
    public boolean hasArray() {
        return false;
    }

    @Override
    public byte[] array() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int arrayOffset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuf clear() {
        throw new UnreplayableOperationException();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int compareTo(ByteBuf buffer) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf copy() {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf copy(int index, int length) {
        this.checkIndex(index, length);
        return this.buffer.copy(index, length);
    }

    @Override
    public ByteBuf discardReadBytes() {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf ensureWritableBytes(int writableBytes) {
        throw new UnreplayableOperationException();
    }

    @Override
    public int ensureWritableBytes(int minWritableBytes, boolean force) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf duplicate() {
        throw new UnreplayableOperationException();
    }

    @Override
    public boolean getBoolean(int index) {
        this.checkIndex(index, 1);
        return this.buffer.getBoolean(index);
    }

    @Override
    public byte getByte(int index) {
        this.checkIndex(index, 1);
        return this.buffer.getByte(index);
    }

    @Override
    public short getUnsignedByte(int index) {
        this.checkIndex(index, 1);
        return this.buffer.getUnsignedByte(index);
    }

    @Override
    public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length) {
        this.checkIndex(index, length);
        this.buffer.getBytes(index, dst, dstIndex, length);
        return this;
    }

    @Override
    public ByteBuf getBytes(int index, byte[] dst) {
        this.checkIndex(index, dst.length);
        this.buffer.getBytes(index, dst);
        return this;
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuffer dst) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length) {
        this.checkIndex(index, length);
        this.buffer.getBytes(index, dst, dstIndex, length);
        return this;
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuf dst, int length) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuf dst) {
        throw new UnreplayableOperationException();
    }

    @Override
    public int getBytes(int index, GatheringByteChannel out, int length) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf getBytes(int index, OutputStream out, int length) {
        throw new UnreplayableOperationException();
    }

    @Override
    public int getInt(int index) {
        this.checkIndex(index, 4);
        return this.buffer.getInt(index);
    }

    @Override
    public long getUnsignedInt(int index) {
        this.checkIndex(index, 4);
        return this.buffer.getUnsignedInt(index);
    }

    @Override
    public long getLong(int index) {
        this.checkIndex(index, 8);
        return this.buffer.getLong(index);
    }

    @Override
    public int getMedium(int index) {
        this.checkIndex(index, 3);
        return this.buffer.getMedium(index);
    }

    @Override
    public int getUnsignedMedium(int index) {
        this.checkIndex(index, 3);
        return this.buffer.getUnsignedMedium(index);
    }

    @Override
    public short getShort(int index) {
        this.checkIndex(index, 2);
        return this.buffer.getShort(index);
    }

    @Override
    public int getUnsignedShort(int index) {
        this.checkIndex(index, 2);
        return this.buffer.getUnsignedShort(index);
    }

    @Override
    public char getChar(int index) {
        this.checkIndex(index, 2);
        return this.buffer.getChar(index);
    }

    @Override
    public float getFloat(int index) {
        this.checkIndex(index, 4);
        return this.buffer.getFloat(index);
    }

    @Override
    public double getDouble(int index) {
        this.checkIndex(index, 8);
        return this.buffer.getDouble(index);
    }

    @Override
    public int hashCode() {
        throw new UnreplayableOperationException();
    }

    @Override
    public int indexOf(int fromIndex, int toIndex, byte value) {
        int endIndex = this.buffer.indexOf(fromIndex, toIndex, value);
        if (endIndex < 0) {
            throw REPLAY;
        }
        return endIndex;
    }

    @Override
    public int indexOf(int fromIndex, int toIndex, ByteBufIndexFinder indexFinder) {
        int endIndex = this.buffer.indexOf(fromIndex, toIndex, indexFinder);
        if (endIndex < 0) {
            throw REPLAY;
        }
        return endIndex;
    }

    @Override
    public int bytesBefore(byte value) {
        int bytes = this.buffer.bytesBefore(value);
        if (bytes < 0) {
            throw REPLAY;
        }
        return bytes;
    }

    @Override
    public int bytesBefore(ByteBufIndexFinder indexFinder) {
        int bytes = this.buffer.bytesBefore(indexFinder);
        if (bytes < 0) {
            throw REPLAY;
        }
        return bytes;
    }

    @Override
    public int bytesBefore(int length, byte value) {
        this.checkReadableBytes(length);
        int bytes = this.buffer.bytesBefore(length, value);
        if (bytes < 0) {
            throw REPLAY;
        }
        return bytes;
    }

    @Override
    public int bytesBefore(int length, ByteBufIndexFinder indexFinder) {
        this.checkReadableBytes(length);
        int bytes = this.buffer.bytesBefore(length, indexFinder);
        if (bytes < 0) {
            throw REPLAY;
        }
        return bytes;
    }

    @Override
    public int bytesBefore(int index, int length, byte value) {
        int bytes = this.buffer.bytesBefore(index, length, value);
        if (bytes < 0) {
            throw REPLAY;
        }
        return bytes;
    }

    @Override
    public int bytesBefore(int index, int length, ByteBufIndexFinder indexFinder) {
        int bytes = this.buffer.bytesBefore(index, length, indexFinder);
        if (bytes < 0) {
            throw REPLAY;
        }
        return bytes;
    }

    @Override
    public ByteBuf markReaderIndex() {
        this.buffer.markReaderIndex();
        return this;
    }

    @Override
    public ByteBuf markWriterIndex() {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteOrder order() {
        return this.buffer.order();
    }

    @Override
    public ByteBuf order(ByteOrder endianness) {
        if (endianness == null) {
            throw new NullPointerException("endianness");
        }
        if (endianness == this.order()) {
            return this;
        }
        return this.swapped;
    }

    @Override
    public boolean readable() {
        return this.terminated ? this.buffer.readable() : true;
    }

    @Override
    public int readableBytes() {
        if (this.terminated) {
            return this.buffer.readableBytes();
        }
        return Integer.MAX_VALUE - this.buffer.readerIndex();
    }

    @Override
    public boolean readBoolean() {
        this.checkReadableBytes(1);
        return this.buffer.readBoolean();
    }

    @Override
    public byte readByte() {
        this.checkReadableBytes(1);
        return this.buffer.readByte();
    }

    @Override
    public short readUnsignedByte() {
        this.checkReadableBytes(1);
        return this.buffer.readUnsignedByte();
    }

    @Override
    public ByteBuf readBytes(byte[] dst, int dstIndex, int length) {
        this.checkReadableBytes(length);
        this.buffer.readBytes(dst, dstIndex, length);
        return this;
    }

    @Override
    public ByteBuf readBytes(byte[] dst) {
        this.checkReadableBytes(dst.length);
        this.buffer.readBytes(dst);
        return this;
    }

    @Override
    public ByteBuf readBytes(ByteBuffer dst) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf readBytes(ByteBuf dst, int dstIndex, int length) {
        this.checkReadableBytes(length);
        this.buffer.readBytes(dst, dstIndex, length);
        return this;
    }

    @Override
    public ByteBuf readBytes(ByteBuf dst, int length) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf readBytes(ByteBuf dst) {
        throw new UnreplayableOperationException();
    }

    @Override
    public int readBytes(GatheringByteChannel out, int length) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf readBytes(int length) {
        this.checkReadableBytes(length);
        return this.buffer.readBytes(length);
    }

    @Override
    public ByteBuf readSlice(int length) {
        this.checkReadableBytes(length);
        return this.buffer.readSlice(length);
    }

    @Override
    public ByteBuf readBytes(OutputStream out, int length) {
        throw new UnreplayableOperationException();
    }

    @Override
    public int readerIndex() {
        return this.buffer.readerIndex();
    }

    @Override
    public ByteBuf readerIndex(int readerIndex) {
        this.buffer.readerIndex(readerIndex);
        return this;
    }

    @Override
    public int readInt() {
        this.checkReadableBytes(4);
        return this.buffer.readInt();
    }

    @Override
    public long readUnsignedInt() {
        this.checkReadableBytes(4);
        return this.buffer.readUnsignedInt();
    }

    @Override
    public long readLong() {
        this.checkReadableBytes(8);
        return this.buffer.readLong();
    }

    @Override
    public int readMedium() {
        this.checkReadableBytes(3);
        return this.buffer.readMedium();
    }

    @Override
    public int readUnsignedMedium() {
        this.checkReadableBytes(3);
        return this.buffer.readUnsignedMedium();
    }

    @Override
    public short readShort() {
        this.checkReadableBytes(2);
        return this.buffer.readShort();
    }

    @Override
    public int readUnsignedShort() {
        this.checkReadableBytes(2);
        return this.buffer.readUnsignedShort();
    }

    @Override
    public char readChar() {
        this.checkReadableBytes(2);
        return this.buffer.readChar();
    }

    @Override
    public float readFloat() {
        this.checkReadableBytes(4);
        return this.buffer.readFloat();
    }

    @Override
    public double readDouble() {
        this.checkReadableBytes(8);
        return this.buffer.readDouble();
    }

    @Override
    public ByteBuf resetReaderIndex() {
        this.buffer.resetReaderIndex();
        return this;
    }

    @Override
    public ByteBuf resetWriterIndex() {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf setBoolean(int index, boolean value) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf setByte(int index, int value) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf setBytes(int index, byte[] src) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuffer src) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuf src, int length) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuf src) {
        throw new UnreplayableOperationException();
    }

    @Override
    public int setBytes(int index, InputStream in, int length) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf setZero(int index, int length) {
        throw new UnreplayableOperationException();
    }

    @Override
    public int setBytes(int index, ScatteringByteChannel in, int length) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf setIndex(int readerIndex, int writerIndex) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf setInt(int index, int value) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf setLong(int index, long value) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf setMedium(int index, int value) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf setShort(int index, int value) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf setChar(int index, int value) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf setFloat(int index, float value) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf setDouble(int index, double value) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf skipBytes(int length) {
        this.checkReadableBytes(length);
        this.buffer.skipBytes(length);
        return this;
    }

    @Override
    public ByteBuf slice() {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf slice(int index, int length) {
        this.checkIndex(index, length);
        return this.buffer.slice(index, length);
    }

    @Override
    public boolean hasNioBuffer() {
        return this.buffer.hasNioBuffer();
    }

    @Override
    public ByteBuffer nioBuffer() {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuffer nioBuffer(int index, int length) {
        this.checkIndex(index, length);
        return this.buffer.nioBuffer(index, length);
    }

    @Override
    public boolean hasNioBuffers() {
        return this.buffer.hasNioBuffers();
    }

    @Override
    public ByteBuffer[] nioBuffers() {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuffer[] nioBuffers(int index, int length) {
        this.checkIndex(index, length);
        return this.buffer.nioBuffers(index, length);
    }

    @Override
    public String toString(int index, int length, Charset charset) {
        this.checkIndex(index, length);
        return this.buffer.toString(index, length, charset);
    }

    @Override
    public String toString(Charset charsetName) {
        throw new UnreplayableOperationException();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '(' + "ridx=" + this.readerIndex() + ", " + "widx=" + this.writerIndex() + ')';
    }

    @Override
    public boolean writable() {
        return false;
    }

    @Override
    public int writableBytes() {
        return 0;
    }

    @Override
    public ByteBuf writeBoolean(boolean value) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf writeByte(int value) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf writeBytes(byte[] src, int srcIndex, int length) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf writeBytes(byte[] src) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf writeBytes(ByteBuffer src) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf writeBytes(ByteBuf src, int srcIndex, int length) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf writeBytes(ByteBuf src, int length) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf writeBytes(ByteBuf src) {
        throw new UnreplayableOperationException();
    }

    @Override
    public int writeBytes(InputStream in, int length) {
        throw new UnreplayableOperationException();
    }

    @Override
    public int writeBytes(ScatteringByteChannel in, int length) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf writeInt(int value) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf writeLong(long value) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf writeMedium(int value) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf writeZero(int length) {
        throw new UnreplayableOperationException();
    }

    @Override
    public int writerIndex() {
        return this.buffer.writerIndex();
    }

    @Override
    public ByteBuf writerIndex(int writerIndex) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf writeShort(int value) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf writeChar(int value) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf writeFloat(float value) {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf writeDouble(double value) {
        throw new UnreplayableOperationException();
    }

    private void checkIndex(int index, int length) {
        if (index + length > this.buffer.writerIndex()) {
            throw REPLAY;
        }
    }

    private void checkReadableBytes(int readableBytes) {
        if (this.buffer.readableBytes() < readableBytes) {
            throw REPLAY;
        }
    }

    @Override
    public ByteBuffer internalNioBuffer() {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuffer[] internalNioBuffers() {
        throw new UnreplayableOperationException();
    }

    @Override
    public void discardSomeReadBytes() {
        throw new UnreplayableOperationException();
    }

    @Override
    public boolean isFreed() {
        return this.buffer.unsafe().isFreed();
    }

    @Override
    public void free() {
        throw new UnreplayableOperationException();
    }

    @Override
    public void suspendIntermediaryDeallocations() {
        throw new UnreplayableOperationException();
    }

    @Override
    public void resumeIntermediaryDeallocations() {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf unwrap() {
        throw new UnreplayableOperationException();
    }

    @Override
    public ByteBuf.Unsafe unsafe() {
        return this;
    }

    static {
        EMPTY_BUFFER.terminate();
    }
}

