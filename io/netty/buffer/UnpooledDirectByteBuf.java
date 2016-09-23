
package io.netty.buffer;

import io.netty.buffer.AbstractByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ChannelBuf;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.util.ArrayDeque;
import java.util.Queue;
import sun.misc.Cleaner;

final class UnpooledDirectByteBuf
extends AbstractByteBuf
implements ByteBuf.Unsafe {
    private static final Field CLEANER_FIELD;
    private final ByteBufAllocator alloc;
    private ByteBuffer buffer;
    private ByteBuffer tmpNioBuf;
    private int capacity;
    private boolean freed;
    private boolean doNotFree;
    private Queue<ByteBuffer> suspendedDeallocations;

    private static void freeDirect(ByteBuffer buffer) {
        if (CLEANER_FIELD == null) {
            return;
        }
        try {
            Cleaner cleaner = (Cleaner)CLEANER_FIELD.get(buffer);
            cleaner.clean();
        }
        catch (Throwable t) {
            // empty catch block
        }
    }

    public UnpooledDirectByteBuf(ByteBufAllocator alloc, int initialCapacity, int maxCapacity) {
        super(maxCapacity);
        if (alloc == null) {
            throw new NullPointerException("alloc");
        }
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity: " + initialCapacity);
        }
        if (maxCapacity < 0) {
            throw new IllegalArgumentException("maxCapacity: " + maxCapacity);
        }
        if (initialCapacity > maxCapacity) {
            throw new IllegalArgumentException(String.format("initialCapacity(%d) > maxCapacity(%d)", initialCapacity, maxCapacity));
        }
        this.alloc = alloc;
        this.setByteBuffer(ByteBuffer.allocateDirect(initialCapacity));
    }

    public UnpooledDirectByteBuf(ByteBufAllocator alloc, ByteBuffer initialBuffer, int maxCapacity) {
        super(maxCapacity);
        if (alloc == null) {
            throw new NullPointerException("alloc");
        }
        if (initialBuffer == null) {
            throw new NullPointerException("initialBuffer");
        }
        if (!initialBuffer.isDirect()) {
            throw new IllegalArgumentException("initialBuffer is not a direct buffer.");
        }
        if (initialBuffer.isReadOnly()) {
            throw new IllegalArgumentException("initialBuffer is a read-only buffer.");
        }
        int initialCapacity = initialBuffer.remaining();
        if (initialCapacity > maxCapacity) {
            throw new IllegalArgumentException(String.format("initialCapacity(%d) > maxCapacity(%d)", initialCapacity, maxCapacity));
        }
        this.alloc = alloc;
        this.doNotFree = true;
        this.setByteBuffer(initialBuffer.slice().order(ByteOrder.BIG_ENDIAN));
        this.writerIndex(initialCapacity);
    }

    private void setByteBuffer(ByteBuffer buffer) {
        ByteBuffer oldBuffer = this.buffer;
        if (oldBuffer != null) {
            if (this.doNotFree) {
                this.doNotFree = false;
            } else if (this.suspendedDeallocations == null) {
                UnpooledDirectByteBuf.freeDirect(oldBuffer);
            } else {
                this.suspendedDeallocations.add(oldBuffer);
            }
        }
        this.buffer = buffer;
        this.tmpNioBuf = null;
        this.capacity = buffer.remaining();
    }

    @Override
    public boolean isDirect() {
        return true;
    }

    @Override
    public int capacity() {
        return this.capacity;
    }

    @Override
    public ByteBuf capacity(int newCapacity) {
        assert (!this.freed);
        if (newCapacity < 0 || newCapacity > this.maxCapacity()) {
            throw new IllegalArgumentException("newCapacity: " + newCapacity);
        }
        int readerIndex = this.readerIndex();
        int writerIndex = this.writerIndex();
        int oldCapacity = this.capacity;
        if (newCapacity > oldCapacity) {
            ByteBuffer oldBuffer = this.buffer;
            ByteBuffer newBuffer = ByteBuffer.allocateDirect(newCapacity);
            oldBuffer.position(readerIndex).limit(writerIndex);
            newBuffer.position(readerIndex).limit(writerIndex);
            newBuffer.put(oldBuffer);
            newBuffer.clear();
            this.setByteBuffer(newBuffer);
        } else if (newCapacity < oldCapacity) {
            ByteBuffer oldBuffer = this.buffer;
            ByteBuffer newBuffer = ByteBuffer.allocateDirect(newCapacity);
            if (readerIndex < newCapacity) {
                if (writerIndex > newCapacity) {
                    writerIndex = newCapacity;
                    this.writerIndex(writerIndex);
                }
                oldBuffer.position(readerIndex).limit(writerIndex);
                newBuffer.position(readerIndex).limit(writerIndex);
                newBuffer.put(oldBuffer);
                newBuffer.clear();
            } else {
                this.setIndex(newCapacity, newCapacity);
            }
            this.setByteBuffer(newBuffer);
        }
        return this;
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
    public boolean hasArray() {
        return false;
    }

    @Override
    public byte[] array() {
        throw new UnsupportedOperationException("direct buffer");
    }

    @Override
    public int arrayOffset() {
        throw new UnsupportedOperationException("direct buffer");
    }

    @Override
    public byte getByte(int index) {
        assert (!this.freed);
        return this.buffer.get(index);
    }

    @Override
    public short getShort(int index) {
        assert (!this.freed);
        return this.buffer.getShort(index);
    }

    @Override
    public int getUnsignedMedium(int index) {
        assert (!this.freed);
        return (this.getByte(index) & 255) << 16 | (this.getByte(index + 1) & 255) << 8 | this.getByte(index + 2) & 255;
    }

    @Override
    public int getInt(int index) {
        assert (!this.freed);
        return this.buffer.getInt(index);
    }

    @Override
    public long getLong(int index) {
        assert (!this.freed);
        return this.buffer.getLong(index);
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length) {
        assert (!this.freed);
        if (dst instanceof UnpooledDirectByteBuf) {
            UnpooledDirectByteBuf bbdst = (UnpooledDirectByteBuf)dst;
            ByteBuffer data = bbdst.internalNioBuffer();
            data.clear().position(dstIndex).limit(dstIndex + length);
            this.getBytes(index, data);
        } else if (this.buffer.hasArray()) {
            dst.setBytes(dstIndex, this.buffer.array(), index + this.buffer.arrayOffset(), length);
        } else {
            dst.setBytes(dstIndex, this, index, length);
        }
        return this;
    }

    @Override
    public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length) {
        assert (!this.freed);
        ByteBuffer tmpBuf = this.internalNioBuffer();
        try {
            tmpBuf.clear().position(index).limit(index + length);
        }
        catch (IllegalArgumentException e) {
            throw new IndexOutOfBoundsException("Too many bytes to read - Need " + (index + length) + ", maximum is " + this.buffer.limit());
        }
        tmpBuf.get(dst, dstIndex, length);
        return this;
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuffer dst) {
        assert (!this.freed);
        int bytesToCopy = Math.min(this.capacity() - index, dst.remaining());
        ByteBuffer tmpBuf = this.internalNioBuffer();
        try {
            tmpBuf.clear().position(index).limit(index + bytesToCopy);
        }
        catch (IllegalArgumentException e) {
            throw new IndexOutOfBoundsException("Too many bytes to read - Need " + (index + bytesToCopy) + ", maximum is " + this.buffer.limit());
        }
        dst.put(tmpBuf);
        return this;
    }

    @Override
    public ByteBuf setByte(int index, int value) {
        assert (!this.freed);
        this.buffer.put(index, (byte)value);
        return this;
    }

    @Override
    public ByteBuf setShort(int index, int value) {
        assert (!this.freed);
        this.buffer.putShort(index, (short)value);
        return this;
    }

    @Override
    public ByteBuf setMedium(int index, int value) {
        assert (!this.freed);
        this.setByte(index, (byte)(value >>> 16));
        this.setByte(index + 1, (byte)(value >>> 8));
        this.setByte(index + 2, (byte)value);
        return this;
    }

    @Override
    public ByteBuf setInt(int index, int value) {
        assert (!this.freed);
        this.buffer.putInt(index, value);
        return this;
    }

    @Override
    public ByteBuf setLong(int index, long value) {
        assert (!this.freed);
        this.buffer.putLong(index, value);
        return this;
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length) {
        assert (!this.freed);
        if (src instanceof UnpooledDirectByteBuf) {
            UnpooledDirectByteBuf bbsrc = (UnpooledDirectByteBuf)src;
            ByteBuffer data = bbsrc.internalNioBuffer();
            data.clear().position(srcIndex).limit(srcIndex + length);
            this.setBytes(index, data);
        } else if (this.buffer.hasArray()) {
            src.getBytes(srcIndex, this.buffer.array(), index + this.buffer.arrayOffset(), length);
        } else {
            src.getBytes(srcIndex, this, index, length);
        }
        return this;
    }

    @Override
    public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length) {
        assert (!this.freed);
        ByteBuffer tmpBuf = this.internalNioBuffer();
        tmpBuf.clear().position(index).limit(index + length);
        tmpBuf.put(src, srcIndex, length);
        return this;
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuffer src) {
        assert (!this.freed);
        ByteBuffer tmpBuf = this.internalNioBuffer();
        if (src == tmpBuf) {
            src = src.duplicate();
        }
        tmpBuf.clear().position(index).limit(index + src.remaining());
        tmpBuf.put(src);
        return this;
    }

    @Override
    public ByteBuf getBytes(int index, OutputStream out, int length) throws IOException {
        assert (!this.freed);
        if (length == 0) {
            return this;
        }
        if (this.buffer.hasArray()) {
            out.write(this.buffer.array(), index + this.buffer.arrayOffset(), length);
        } else {
            byte[] tmp = new byte[length];
            ByteBuffer tmpBuf = this.internalNioBuffer();
            tmpBuf.clear().position(index);
            tmpBuf.get(tmp);
            out.write(tmp);
        }
        return this;
    }

    @Override
    public int getBytes(int index, GatheringByteChannel out, int length) throws IOException {
        assert (!this.freed);
        if (length == 0) {
            return 0;
        }
        ByteBuffer tmpBuf = this.internalNioBuffer();
        tmpBuf.clear().position(index).limit(index + length);
        return out.write(tmpBuf);
    }

    @Override
    public int setBytes(int index, InputStream in, int length) throws IOException {
        assert (!this.freed);
        if (this.buffer.hasArray()) {
            return in.read(this.buffer.array(), this.buffer.arrayOffset() + index, length);
        }
        byte[] tmp = new byte[length];
        int readBytes = in.read(tmp);
        ByteBuffer tmpNioBuf = this.internalNioBuffer();
        tmpNioBuf.clear().position(index);
        tmpNioBuf.put(tmp);
        return readBytes;
    }

    @Override
    public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException {
        assert (!this.freed);
        ByteBuffer tmpNioBuf = this.internalNioBuffer();
        tmpNioBuf.clear().position(index).limit(index + length);
        try {
            return in.read(tmpNioBuf);
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
        if (index == 0 && length == this.capacity()) {
            return this.buffer.duplicate();
        }
        return ((ByteBuffer)this.internalNioBuffer().clear().position(index).limit(index + length)).slice();
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
    public ByteBuf copy(int index, int length) {
        ByteBuffer src;
        assert (!this.freed);
        try {
            src = (ByteBuffer)this.internalNioBuffer().clear().position(index).limit(index + length);
        }
        catch (IllegalArgumentException e) {
            throw new IndexOutOfBoundsException("Too many bytes to read - Need " + (index + length));
        }
        ByteBuffer dst = src.isDirect() ? ByteBuffer.allocateDirect(length) : ByteBuffer.allocate(length);
        dst.put(src);
        dst.order(this.order());
        dst.clear();
        return new UnpooledDirectByteBuf(this.alloc(), dst, this.maxCapacity());
    }

    @Override
    public ByteBuffer internalNioBuffer() {
        ByteBuffer tmpNioBuf = this.tmpNioBuf;
        if (tmpNioBuf == null) {
            this.tmpNioBuf = tmpNioBuf = this.buffer.duplicate();
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
        if (readerIndex > 0 && readerIndex >= this.capacity >>> 1) {
            this.discardReadBytes();
        }
    }

    @Override
    public boolean isFreed() {
        return this.freed;
    }

    @Override
    public void free() {
        if (this.freed) {
            return;
        }
        this.freed = true;
        if (this.doNotFree) {
            return;
        }
        this.resumeIntermediaryDeallocations();
        UnpooledDirectByteBuf.freeDirect(this.buffer);
    }

    @Override
    public void suspendIntermediaryDeallocations() {
        if (this.suspendedDeallocations == null) {
            this.suspendedDeallocations = new ArrayDeque<ByteBuffer>(2);
        }
    }

    @Override
    public void resumeIntermediaryDeallocations() {
        if (this.suspendedDeallocations == null) {
            return;
        }
        Queue<ByteBuffer> suspendedDeallocations = this.suspendedDeallocations;
        this.suspendedDeallocations = null;
        for (ByteBuffer buf : suspendedDeallocations) {
            UnpooledDirectByteBuf.freeDirect(buf);
        }
    }

    @Override
    public ByteBuf unwrap() {
        return null;
    }

    @Override
    public ByteBuf.Unsafe unsafe() {
        return this;
    }

    static {
        Field cleanerField;
        ByteBuffer direct = ByteBuffer.allocateDirect(1);
        try {
            cleanerField = direct.getClass().getDeclaredField("cleaner");
            cleanerField.setAccessible(true);
            Cleaner cleaner = (Cleaner)cleanerField.get(direct);
            cleaner.clean();
        }
        catch (Throwable t) {
            cleanerField = null;
        }
        CLEANER_FIELD = cleanerField;
    }
}

