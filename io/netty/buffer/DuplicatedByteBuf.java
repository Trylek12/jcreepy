
package io.netty.buffer;

import io.netty.buffer.AbstractByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ChannelBuf;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;

final class DuplicatedByteBuf
extends AbstractByteBuf
implements ByteBuf.Unsafe {
    private final ByteBuf buffer;

    public DuplicatedByteBuf(ByteBuf buffer) {
        super(buffer.maxCapacity());
        this.buffer = buffer instanceof DuplicatedByteBuf ? ((DuplicatedByteBuf)buffer).buffer : buffer;
        this.setIndex(buffer.readerIndex(), buffer.writerIndex());
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
        return this.buffer.capacity();
    }

    @Override
    public ByteBuf capacity(int newCapacity) {
        this.buffer.capacity(newCapacity);
        return this;
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
        return this.buffer.arrayOffset();
    }

    @Override
    public byte getByte(int index) {
        return this.buffer.getByte(index);
    }

    @Override
    public short getShort(int index) {
        return this.buffer.getShort(index);
    }

    @Override
    public int getUnsignedMedium(int index) {
        return this.buffer.getUnsignedMedium(index);
    }

    @Override
    public int getInt(int index) {
        return this.buffer.getInt(index);
    }

    @Override
    public long getLong(int index) {
        return this.buffer.getLong(index);
    }

    @Override
    public ByteBuf copy(int index, int length) {
        return this.buffer.copy(index, length);
    }

    @Override
    public ByteBuf slice(int index, int length) {
        return this.buffer.slice(index, length);
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length) {
        this.buffer.getBytes(index, dst, dstIndex, length);
        return this;
    }

    @Override
    public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length) {
        this.buffer.getBytes(index, dst, dstIndex, length);
        return this;
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuffer dst) {
        this.buffer.getBytes(index, dst);
        return this;
    }

    @Override
    public ByteBuf setByte(int index, int value) {
        this.buffer.setByte(index, value);
        return this;
    }

    @Override
    public ByteBuf setShort(int index, int value) {
        this.buffer.setShort(index, value);
        return this;
    }

    @Override
    public ByteBuf setMedium(int index, int value) {
        this.buffer.setMedium(index, value);
        return this;
    }

    @Override
    public ByteBuf setInt(int index, int value) {
        this.buffer.setInt(index, value);
        return this;
    }

    @Override
    public ByteBuf setLong(int index, long value) {
        this.buffer.setLong(index, value);
        return this;
    }

    @Override
    public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length) {
        this.buffer.setBytes(index, src, srcIndex, length);
        return this;
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length) {
        this.buffer.setBytes(index, src, srcIndex, length);
        return this;
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuffer src) {
        this.buffer.setBytes(index, src);
        return this;
    }

    @Override
    public ByteBuf getBytes(int index, OutputStream out, int length) throws IOException {
        this.buffer.getBytes(index, out, length);
        return this;
    }

    @Override
    public int getBytes(int index, GatheringByteChannel out, int length) throws IOException {
        return this.buffer.getBytes(index, out, length);
    }

    @Override
    public int setBytes(int index, InputStream in, int length) throws IOException {
        return this.buffer.setBytes(index, in, length);
    }

    @Override
    public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException {
        return this.buffer.setBytes(index, in, length);
    }

    @Override
    public boolean hasNioBuffer() {
        return this.buffer.hasNioBuffer();
    }

    @Override
    public ByteBuffer nioBuffer(int index, int length) {
        return this.buffer.nioBuffer(index, length);
    }

    @Override
    public boolean hasNioBuffers() {
        return this.buffer.hasNioBuffers();
    }

    @Override
    public ByteBuffer[] nioBuffers(int offset, int length) {
        return this.buffer.nioBuffers(offset, length);
    }

    @Override
    public ByteBuffer internalNioBuffer() {
        return this.buffer.unsafe().internalNioBuffer();
    }

    @Override
    public ByteBuffer[] internalNioBuffers() {
        return this.buffer.unsafe().internalNioBuffers();
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

