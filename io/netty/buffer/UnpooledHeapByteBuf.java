
package io.netty.buffer;

import io.netty.buffer.AbstractByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ChannelBuf;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;

final class UnpooledHeapByteBuf
extends AbstractByteBuf
implements ByteBuf.Unsafe {
    private final ByteBufAllocator alloc;
    private byte[] array;
    private ByteBuffer tmpNioBuf;
    private boolean freed;

    public UnpooledHeapByteBuf(ByteBufAllocator alloc, int initialCapacity, int maxCapacity) {
        this(alloc, new byte[initialCapacity], 0, 0, maxCapacity);
    }

    public UnpooledHeapByteBuf(ByteBufAllocator alloc, byte[] initialArray, int maxCapacity) {
        this(alloc, initialArray, 0, initialArray.length, maxCapacity);
    }

    private UnpooledHeapByteBuf(ByteBufAllocator alloc, byte[] initialArray, int readerIndex, int writerIndex, int maxCapacity) {
        super(maxCapacity);
        if (alloc == null) {
            throw new NullPointerException("alloc");
        }
        if (initialArray == null) {
            throw new NullPointerException("initialArray");
        }
        if (initialArray.length > maxCapacity) {
            throw new IllegalArgumentException(String.format("initialCapacity(%d) > maxCapacity(%d)", initialArray.length, maxCapacity));
        }
        this.alloc = alloc;
        this.setArray(initialArray);
        this.setIndex(readerIndex, writerIndex);
    }

    private void setArray(byte[] initialArray) {
        this.array = initialArray;
        this.tmpNioBuf = null;
    }

    @Override
    public ByteBufAllocator alloc() {
        return this.alloc;
    }

    @Override
    public ByteOrder order() {
        return ByteOrder.BIG_ENDIAN;
    }

    @Override
    public boolean isDirect() {
        return false;
    }

    @Override
    public int capacity() {
        return this.array.length;
    }

    @Override
    public ByteBuf capacity(int newCapacity) {
        assert (!this.freed);
        if (newCapacity < 0 || newCapacity > this.maxCapacity()) {
            throw new IllegalArgumentException("newCapacity: " + newCapacity);
        }
        int oldCapacity = this.array.length;
        if (newCapacity > oldCapacity) {
            byte[] newArray = new byte[newCapacity];
            System.arraycopy(this.array, this.readerIndex(), newArray, this.readerIndex(), this.readableBytes());
            this.setArray(newArray);
        } else if (newCapacity < oldCapacity) {
            byte[] newArray = new byte[newCapacity];
            int readerIndex = this.readerIndex();
            if (readerIndex < newCapacity) {
                int writerIndex = this.writerIndex();
                if (writerIndex > newCapacity) {
                    writerIndex = newCapacity;
                    this.writerIndex(writerIndex);
                }
                System.arraycopy(this.array, readerIndex, newArray, readerIndex, writerIndex - readerIndex);
            } else {
                this.setIndex(newCapacity, newCapacity);
            }
            this.setArray(newArray);
        }
        return this;
    }

    @Override
    public boolean hasArray() {
        return true;
    }

    @Override
    public byte[] array() {
        assert (!this.freed);
        return this.array;
    }

    @Override
    public int arrayOffset() {
        return 0;
    }

    @Override
    public byte getByte(int index) {
        assert (!this.freed);
        return this.array[index];
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length) {
        assert (!this.freed);
        if (dst instanceof UnpooledHeapByteBuf) {
            this.getBytes(index, ((UnpooledHeapByteBuf)dst).array, dstIndex, length);
        } else {
            dst.setBytes(dstIndex, this.array, index, length);
        }
        return this;
    }

    @Override
    public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length) {
        assert (!this.freed);
        System.arraycopy(this.array, index, dst, dstIndex, length);
        return this;
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuffer dst) {
        assert (!this.freed);
        dst.put(this.array, index, Math.min(this.capacity() - index, dst.remaining()));
        return this;
    }

    @Override
    public ByteBuf getBytes(int index, OutputStream out, int length) throws IOException {
        assert (!this.freed);
        out.write(this.array, index, length);
        return this;
    }

    @Override
    public int getBytes(int index, GatheringByteChannel out, int length) throws IOException {
        assert (!this.freed);
        return out.write((ByteBuffer)this.internalNioBuffer().clear().position(index).limit(index + length));
    }

    @Override
    public ByteBuf setByte(int index, int value) {
        assert (!this.freed);
        this.array[index] = (byte)value;
        return this;
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length) {
        assert (!this.freed);
        if (src instanceof UnpooledHeapByteBuf) {
            this.setBytes(index, ((UnpooledHeapByteBuf)src).array, srcIndex, length);
        } else {
            src.getBytes(srcIndex, this.array, index, length);
        }
        return this;
    }

    @Override
    public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length) {
        assert (!this.freed);
        System.arraycopy(src, srcIndex, this.array, index, length);
        return this;
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuffer src) {
        assert (!this.freed);
        src.get(this.array, index, src.remaining());
        return this;
    }

    @Override
    public int setBytes(int index, InputStream in, int length) throws IOException {
        assert (!this.freed);
        return in.read(this.array, index, length);
    }

    @Override
    public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException {
        assert (!this.freed);
        try {
            return in.read((ByteBuffer)this.internalNioBuffer().clear().position(index).limit(index + length));
        }
        catch (ClosedChannelException e) {
            return -1;
        }
    }

    @Override
    public boolean hasNioBuffer() {
        return true;
    }

    @Override
    public ByteBuffer nioBuffer(int index, int length) {
        assert (!this.freed);
        return ByteBuffer.wrap(this.array, index, length);
    }

    @Override
    public boolean hasNioBuffers() {
        return false;
    }

    @Override
    public ByteBuffer[] nioBuffers(int offset, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public short getShort(int index) {
        assert (!this.freed);
        return (short)(this.array[index] << 8 | this.array[index + 1] & 255);
    }

    @Override
    public int getUnsignedMedium(int index) {
        assert (!this.freed);
        return (this.array[index] & 255) << 16 | (this.array[index + 1] & 255) << 8 | this.array[index + 2] & 255;
    }

    @Override
    public int getInt(int index) {
        assert (!this.freed);
        return (this.array[index] & 255) << 24 | (this.array[index + 1] & 255) << 16 | (this.array[index + 2] & 255) << 8 | this.array[index + 3] & 255;
    }

    @Override
    public long getLong(int index) {
        assert (!this.freed);
        return ((long)this.array[index] & 255) << 56 | ((long)this.array[index + 1] & 255) << 48 | ((long)this.array[index + 2] & 255) << 40 | ((long)this.array[index + 3] & 255) << 32 | ((long)this.array[index + 4] & 255) << 24 | ((long)this.array[index + 5] & 255) << 16 | ((long)this.array[index + 6] & 255) << 8 | (long)this.array[index + 7] & 255;
    }

    @Override
    public ByteBuf setShort(int index, int value) {
        assert (!this.freed);
        this.array[index] = (byte)(value >>> 8);
        this.array[index + 1] = (byte)value;
        return this;
    }

    @Override
    public ByteBuf setMedium(int index, int value) {
        assert (!this.freed);
        this.array[index] = (byte)(value >>> 16);
        this.array[index + 1] = (byte)(value >>> 8);
        this.array[index + 2] = (byte)value;
        return this;
    }

    @Override
    public ByteBuf setInt(int index, int value) {
        assert (!this.freed);
        this.array[index] = (byte)(value >>> 24);
        this.array[index + 1] = (byte)(value >>> 16);
        this.array[index + 2] = (byte)(value >>> 8);
        this.array[index + 3] = (byte)value;
        return this;
    }

    @Override
    public ByteBuf setLong(int index, long value) {
        assert (!this.freed);
        this.array[index] = (byte)(value >>> 56);
        this.array[index + 1] = (byte)(value >>> 48);
        this.array[index + 2] = (byte)(value >>> 40);
        this.array[index + 3] = (byte)(value >>> 32);
        this.array[index + 4] = (byte)(value >>> 24);
        this.array[index + 5] = (byte)(value >>> 16);
        this.array[index + 6] = (byte)(value >>> 8);
        this.array[index + 7] = (byte)value;
        return this;
    }

    @Override
    public ByteBuf copy(int index, int length) {
        assert (!this.freed);
        if (index < 0 || length < 0 || index + length > this.array.length) {
            throw new IndexOutOfBoundsException("Too many bytes to copy - Need " + (index + length) + ", maximum is " + this.array.length);
        }
        byte[] copiedArray = new byte[length];
        System.arraycopy(this.array, index, copiedArray, 0, length);
        return new UnpooledHeapByteBuf(this.alloc(), copiedArray, this.maxCapacity());
    }

    @Override
    public ByteBuffer internalNioBuffer() {
        ByteBuffer tmpNioBuf = this.tmpNioBuf;
        if (tmpNioBuf == null) {
            this.tmpNioBuf = tmpNioBuf = ByteBuffer.wrap(this.array);
        }
        return tmpNioBuf;
    }

    @Override
    public ByteBuffer[] internalNioBuffers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void discardSomeReadBytes() {
        int readerIndex = this.readerIndex();
        if (readerIndex == this.writerIndex()) {
            this.discardReadBytes();
            return;
        }
        if (readerIndex > 0 && readerIndex >= this.capacity() >>> 1) {
            this.discardReadBytes();
        }
    }

    @Override
    public boolean isFreed() {
        return this.freed;
    }

    @Override
    public void free() {
        this.freed = true;
    }

    @Override
    public void suspendIntermediaryDeallocations() {
    }

    @Override
    public void resumeIntermediaryDeallocations() {
    }

    @Override
    public ByteBuf unwrap() {
        return null;
    }

    @Override
    public ByteBuf.Unsafe unsafe() {
        return this;
    }
}

