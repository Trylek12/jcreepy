
package io.netty.buffer;

import io.netty.buffer.AbstractByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ChannelBuf;
import io.netty.buffer.DuplicatedByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;

final class SlicedByteBuf
extends AbstractByteBuf
implements ByteBuf.Unsafe {
    private final ByteBuf buffer;
    private final int adjustment;
    private final int length;

    public SlicedByteBuf(ByteBuf buffer, int index, int length) {
        super(length);
        if (index < 0 || index > buffer.capacity()) {
            throw new IndexOutOfBoundsException("Invalid index of " + index + ", maximum is " + buffer.capacity());
        }
        if (index + length > buffer.capacity()) {
            throw new IndexOutOfBoundsException("Invalid combined index of " + (index + length) + ", maximum is " + buffer.capacity());
        }
        if (buffer instanceof SlicedByteBuf) {
            this.buffer = ((SlicedByteBuf)buffer).buffer;
            this.adjustment = ((SlicedByteBuf)buffer).adjustment + index;
        } else if (buffer instanceof DuplicatedByteBuf) {
            this.buffer = buffer.unwrap();
            this.adjustment = index;
        } else {
            this.buffer = buffer;
            this.adjustment = index;
        }
        this.length = length;
        this.writerIndex(length);
    }

    @Override
    public ByteBuf unwrap() {
        return this.buffer;
    }

    @Override
    public ByteBufAllocator alloc() {
        return this.buffer.alloc();
    }

    @Override
    public ByteOrder order() {
        return this.buffer.order();
    }

    @Override
    public boolean isDirect() {
        return this.buffer.isDirect();
    }

    @Override
    public int capacity() {
        return this.length;
    }

    @Override
    public ByteBuf capacity(int newCapacity) {
        throw new UnsupportedOperationException("sliced buffer");
    }

    @Override
    public boolean hasArray() {
        return this.buffer.hasArray();
    }

    @Override
    public byte[] array() {
        return this.buffer.array();
    }

    @Override
    public int arrayOffset() {
        return this.buffer.arrayOffset() + this.adjustment;
    }

    @Override
    public byte getByte(int index) {
        this.checkIndex(index);
        return this.buffer.getByte(index + this.adjustment);
    }

    @Override
    public short getShort(int index) {
        this.checkIndex(index, 2);
        return this.buffer.getShort(index + this.adjustment);
    }

    @Override
    public int getUnsignedMedium(int index) {
        this.checkIndex(index, 3);
        return this.buffer.getUnsignedMedium(index + this.adjustment);
    }

    @Override
    public int getInt(int index) {
        this.checkIndex(index, 4);
        return this.buffer.getInt(index + this.adjustment);
    }

    @Override
    public long getLong(int index) {
        this.checkIndex(index, 8);
        return this.buffer.getLong(index + this.adjustment);
    }

    @Override
    public ByteBuf duplicate() {
        SlicedByteBuf duplicate = new SlicedByteBuf(this.buffer, this.adjustment, this.length);
        duplicate.setIndex(this.readerIndex(), this.writerIndex());
        return duplicate;
    }

    @Override
    public ByteBuf copy(int index, int length) {
        this.checkIndex(index, length);
        return this.buffer.copy(index + this.adjustment, length);
    }

    @Override
    public ByteBuf slice(int index, int length) {
        this.checkIndex(index, length);
        if (length == 0) {
            return Unpooled.EMPTY_BUFFER;
        }
        return new SlicedByteBuf(this.buffer, index + this.adjustment, length);
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length) {
        this.checkIndex(index, length);
        this.buffer.getBytes(index + this.adjustment, dst, dstIndex, length);
        return this;
    }

    @Override
    public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length) {
        this.checkIndex(index, length);
        this.buffer.getBytes(index + this.adjustment, dst, dstIndex, length);
        return this;
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuffer dst) {
        this.checkIndex(index, dst.remaining());
        this.buffer.getBytes(index + this.adjustment, dst);
        return this;
    }

    @Override
    public ByteBuf setByte(int index, int value) {
        this.checkIndex(index);
        this.buffer.setByte(index + this.adjustment, value);
        return this;
    }

    @Override
    public ByteBuf setShort(int index, int value) {
        this.checkIndex(index, 2);
        this.buffer.setShort(index + this.adjustment, value);
        return this;
    }

    @Override
    public ByteBuf setMedium(int index, int value) {
        this.checkIndex(index, 3);
        this.buffer.setMedium(index + this.adjustment, value);
        return this;
    }

    @Override
    public ByteBuf setInt(int index, int value) {
        this.checkIndex(index, 4);
        this.buffer.setInt(index + this.adjustment, value);
        return this;
    }

    @Override
    public ByteBuf setLong(int index, long value) {
        this.checkIndex(index, 8);
        this.buffer.setLong(index + this.adjustment, value);
        return this;
    }

    @Override
    public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length) {
        this.checkIndex(index, length);
        this.buffer.setBytes(index + this.adjustment, src, srcIndex, length);
        return this;
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length) {
        this.checkIndex(index, length);
        this.buffer.setBytes(index + this.adjustment, src, srcIndex, length);
        return this;
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuffer src) {
        this.checkIndex(index, src.remaining());
        this.buffer.setBytes(index + this.adjustment, src);
        return this;
    }

    @Override
    public ByteBuf getBytes(int index, OutputStream out, int length) throws IOException {
        this.checkIndex(index, length);
        this.buffer.getBytes(index + this.adjustment, out, length);
        return this;
    }

    @Override
    public int getBytes(int index, GatheringByteChannel out, int length) throws IOException {
        this.checkIndex(index, length);
        return this.buffer.getBytes(index + this.adjustment, out, length);
    }

    @Override
    public int setBytes(int index, InputStream in, int length) throws IOException {
        this.checkIndex(index, length);
        return this.buffer.setBytes(index + this.adjustment, in, length);
    }

    @Override
    public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException {
        this.checkIndex(index, length);
        return this.buffer.setBytes(index + this.adjustment, in, length);
    }

    @Override
    public boolean hasNioBuffer() {
        return this.buffer.hasNioBuffer();
    }

    @Override
    public ByteBuffer nioBuffer(int index, int length) {
        this.checkIndex(index, length);
        return this.buffer.nioBuffer(index + this.adjustment, length);
    }

    @Override
    public boolean hasNioBuffers() {
        return this.buffer.hasNioBuffers();
    }

    @Override
    public ByteBuffer[] nioBuffers(int index, int length) {
        this.checkIndex(index, length);
        return this.buffer.nioBuffers(index, length);
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= this.capacity()) {
            throw new IndexOutOfBoundsException("Invalid index: " + index + ", maximum is " + this.capacity());
        }
    }

    private void checkIndex(int startIndex, int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length is negative: " + length);
        }
        if (startIndex < 0) {
            throw new IndexOutOfBoundsException("startIndex cannot be negative");
        }
        if (startIndex + length > this.capacity()) {
            throw new IndexOutOfBoundsException("Index too big - Bytes needed: " + (startIndex + length) + ", maximum is " + this.capacity());
        }
    }

    @Override
    public ByteBuffer internalNioBuffer() {
        return this.buffer.nioBuffer(this.adjustment, this.length);
    }

    @Override
    public ByteBuffer[] internalNioBuffers() {
        return this.buffer.nioBuffers(this.adjustment, this.length);
    }

    @Override
    public void discardSomeReadBytes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isFreed() {
        return this.buffer.unsafe().isFreed();
    }

    @Override
    public void free() {
    }

    @Override
    public void suspendIntermediaryDeallocations() {
    }

    @Override
    public void resumeIntermediaryDeallocations() {
    }

    @Override
    public ByteBuf.Unsafe unsafe() {
        return this;
    }
}

