
package io.netty.buffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufIndexFinder;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.ChannelBufType;
import io.netty.buffer.DuplicatedByteBuf;
import io.netty.buffer.SlicedByteBuf;
import io.netty.buffer.SwappedByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;

public abstract class AbstractByteBuf
implements ByteBuf {
    private int readerIndex;
    private int writerIndex;
    private int markedReaderIndex;
    private int markedWriterIndex;
    private final int maxCapacity;
    private SwappedByteBuf swappedBuf;

    protected AbstractByteBuf(int maxCapacity) {
        if (maxCapacity < 0) {
            throw new IllegalArgumentException("maxCapacity: " + maxCapacity + " (expected: >= 0)");
        }
        this.maxCapacity = maxCapacity;
    }

    @Override
    public ChannelBufType type() {
        return ChannelBufType.BYTE;
    }

    @Override
    public int maxCapacity() {
        return this.maxCapacity;
    }

    @Override
    public int readerIndex() {
        return this.readerIndex;
    }

    @Override
    public ByteBuf readerIndex(int readerIndex) {
        if (readerIndex < 0 || readerIndex > this.writerIndex) {
            throw new IndexOutOfBoundsException("Invalid readerIndex: " + readerIndex + " - Maximum is " + this.writerIndex);
        }
        this.readerIndex = readerIndex;
        return this;
    }

    @Override
    public int writerIndex() {
        return this.writerIndex;
    }

    @Override
    public ByteBuf writerIndex(int writerIndex) {
        if (writerIndex < this.readerIndex || writerIndex > this.capacity()) {
            throw new IndexOutOfBoundsException("Invalid writerIndex: " + writerIndex + " - Maximum is " + this.readerIndex + " or " + this.capacity());
        }
        this.writerIndex = writerIndex;
        return this;
    }

    @Override
    public ByteBuf setIndex(int readerIndex, int writerIndex) {
        if (readerIndex < 0 || readerIndex > writerIndex || writerIndex > this.capacity()) {
            throw new IndexOutOfBoundsException("Invalid indexes: readerIndex is " + readerIndex + ", writerIndex is " + writerIndex + ", capacity is " + this.capacity());
        }
        this.readerIndex = readerIndex;
        this.writerIndex = writerIndex;
        return this;
    }

    @Override
    public ByteBuf clear() {
        this.writerIndex = 0;
        this.readerIndex = 0;
        return this;
    }

    @Override
    public boolean readable() {
        return this.writerIndex > this.readerIndex;
    }

    @Override
    public boolean writable() {
        return this.writableBytes() > 0;
    }

    @Override
    public int readableBytes() {
        return this.writerIndex - this.readerIndex;
    }

    @Override
    public int writableBytes() {
        return this.capacity() - this.writerIndex;
    }

    @Override
    public ByteBuf markReaderIndex() {
        this.markedReaderIndex = this.readerIndex;
        return this;
    }

    @Override
    public ByteBuf resetReaderIndex() {
        this.readerIndex(this.markedReaderIndex);
        return this;
    }

    @Override
    public ByteBuf markWriterIndex() {
        this.markedWriterIndex = this.writerIndex;
        return this;
    }

    @Override
    public ByteBuf resetWriterIndex() {
        this.writerIndex = this.markedWriterIndex;
        return this;
    }

    @Override
    public ByteBuf discardReadBytes() {
        if (this.readerIndex == 0) {
            return this;
        }
        if (this.readerIndex != this.writerIndex) {
            this.setBytes(0, this, this.readerIndex, this.writerIndex - this.readerIndex);
            this.writerIndex -= this.readerIndex;
            this.adjustMarkers(this.readerIndex);
            this.readerIndex = 0;
        } else {
            this.adjustMarkers(this.readerIndex);
            this.readerIndex = 0;
            this.writerIndex = 0;
        }
        return this;
    }

    protected void adjustMarkers(int decrement) {
        this.markedReaderIndex = Math.max(this.markedReaderIndex - decrement, 0);
        this.markedWriterIndex = Math.max(this.markedWriterIndex - decrement, 0);
    }

    @Override
    public ByteBuf ensureWritableBytes(int minWritableBytes) {
        if (minWritableBytes < 0) {
            throw new IllegalArgumentException(String.format("minWritableBytes: %d (expected: >= 0)", minWritableBytes));
        }
        if (minWritableBytes <= this.writableBytes()) {
            return this;
        }
        if (minWritableBytes > this.maxCapacity - this.writerIndex) {
            throw new IndexOutOfBoundsException(String.format("writerIndex(%d) + minWritableBytes(%d) exceeds maxCapacity(%d)", this.writerIndex, minWritableBytes, this.maxCapacity));
        }
        int newCapacity = this.calculateNewCapacity(this.writerIndex + minWritableBytes);
        this.capacity(newCapacity);
        return this;
    }

    @Override
    public int ensureWritableBytes(int minWritableBytes, boolean force) {
        if (minWritableBytes < 0) {
            throw new IllegalArgumentException(String.format("minWritableBytes: %d (expected: >= 0)", minWritableBytes));
        }
        if (minWritableBytes <= this.writableBytes()) {
            return 0;
        }
        if (minWritableBytes > this.maxCapacity - this.writerIndex && force) {
            if (this.capacity() == this.maxCapacity()) {
                return 1;
            }
            this.capacity(this.maxCapacity());
            return 3;
        }
        int newCapacity = this.calculateNewCapacity(this.writerIndex + minWritableBytes);
        this.capacity(newCapacity);
        return 2;
    }

    private int calculateNewCapacity(int minNewCapacity) {
        int newCapacity;
        int maxCapacity = this.maxCapacity;
        int threshold = 4194304;
        if (minNewCapacity == 4194304) {
            return 4194304;
        }
        if (minNewCapacity > 4194304) {
            int newCapacity2 = minNewCapacity / 4194304 * 4194304;
            newCapacity2 = newCapacity2 > maxCapacity - 4194304 ? maxCapacity : (newCapacity2 += 4194304);
            return newCapacity2;
        }
        for (newCapacity = 64; newCapacity < minNewCapacity; newCapacity <<= 1) {
        }
        return Math.min(newCapacity, maxCapacity);
    }

    @Override
    public ByteBuf order(ByteOrder endianness) {
        if (endianness == null) {
            throw new NullPointerException("endianness");
        }
        if (endianness == this.order() || this.capacity() == 0) {
            return this;
        }
        SwappedByteBuf swappedBuf = this.swappedBuf;
        if (swappedBuf == null) {
            this.swappedBuf = swappedBuf = new SwappedByteBuf(this);
        }
        return swappedBuf;
    }

    @Override
    public boolean getBoolean(int index) {
        return this.getByte(index) != 0;
    }

    @Override
    public short getUnsignedByte(int index) {
        return (short)(this.getByte(index) & 255);
    }

    @Override
    public int getUnsignedShort(int index) {
        return this.getShort(index) & 65535;
    }

    @Override
    public int getMedium(int index) {
        int value = this.getUnsignedMedium(index);
        if ((value & 8388608) != 0) {
            value |= -16777216;
        }
        return value;
    }

    @Override
    public long getUnsignedInt(int index) {
        return (long)this.getInt(index) & 0xFFFFFFFFL;
    }

    @Override
    public char getChar(int index) {
        return (char)this.getShort(index);
    }

    @Override
    public float getFloat(int index) {
        return Float.intBitsToFloat(this.getInt(index));
    }

    @Override
    public double getDouble(int index) {
        return Double.longBitsToDouble(this.getLong(index));
    }

    @Override
    public ByteBuf getBytes(int index, byte[] dst) {
        this.getBytes(index, dst, 0, dst.length);
        return this;
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuf dst) {
        this.getBytes(index, dst, dst.writableBytes());
        return this;
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuf dst, int length) {
        if (length > dst.writableBytes()) {
            throw new IndexOutOfBoundsException("Too many bytes to be read: Need " + length + ", maximum is " + dst.writableBytes());
        }
        this.getBytes(index, dst, dst.writerIndex(), length);
        dst.writerIndex(dst.writerIndex() + length);
        return this;
    }

    @Override
    public ByteBuf setBoolean(int index, boolean value) {
        this.setByte(index, value ? 1 : 0);
        return this;
    }

    @Override
    public ByteBuf setChar(int index, int value) {
        this.setShort(index, value);
        return this;
    }

    @Override
    public ByteBuf setFloat(int index, float value) {
        this.setInt(index, Float.floatToRawIntBits(value));
        return this;
    }

    @Override
    public ByteBuf setDouble(int index, double value) {
        this.setLong(index, Double.doubleToRawLongBits(value));
        return this;
    }

    @Override
    public ByteBuf setBytes(int index, byte[] src) {
        this.setBytes(index, src, 0, src.length);
        return this;
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuf src) {
        this.setBytes(index, src, src.readableBytes());
        return this;
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuf src, int length) {
        if (length > src.readableBytes()) {
            throw new IndexOutOfBoundsException("Too many bytes to write: Need " + length + ", maximum is " + src.readableBytes());
        }
        this.setBytes(index, src, src.readerIndex(), length);
        src.readerIndex(src.readerIndex() + length);
        return this;
    }

    @Override
    public ByteBuf setZero(int index, int length) {
        int i;
        if (length == 0) {
            return this;
        }
        if (length < 0) {
            throw new IllegalArgumentException("length must be 0 or greater than 0.");
        }
        int nLong = length >>> 3;
        int nBytes = length & 7;
        for (i = nLong; i > 0; --i) {
            this.setLong(index, 0);
            index += 8;
        }
        if (nBytes == 4) {
            this.setInt(index, 0);
        } else if (nBytes < 4) {
            for (i = nBytes; i > 0; --i) {
                this.setByte(index, 0);
                ++index;
            }
        } else {
            this.setInt(index, 0);
            index += 4;
            for (i = nBytes - 4; i > 0; --i) {
                this.setByte(index, 0);
                ++index;
            }
        }
        return this;
    }

    @Override
    public byte readByte() {
        if (this.readerIndex == this.writerIndex) {
            throw new IndexOutOfBoundsException("Readable byte limit exceeded: " + this.readerIndex);
        }
        return this.getByte(this.readerIndex++);
    }

    @Override
    public boolean readBoolean() {
        return this.readByte() != 0;
    }

    @Override
    public short readUnsignedByte() {
        return (short)(this.readByte() & 255);
    }

    @Override
    public short readShort() {
        this.checkReadableBytes(2);
        short v = this.getShort(this.readerIndex);
        this.readerIndex += 2;
        return v;
    }

    @Override
    public int readUnsignedShort() {
        return this.readShort() & 65535;
    }

    @Override
    public int readMedium() {
        int value = this.readUnsignedMedium();
        if ((value & 8388608) != 0) {
            value |= -16777216;
        }
        return value;
    }

    @Override
    public int readUnsignedMedium() {
        this.checkReadableBytes(3);
        int v = this.getUnsignedMedium(this.readerIndex);
        this.readerIndex += 3;
        return v;
    }

    @Override
    public int readInt() {
        this.checkReadableBytes(4);
        int v = this.getInt(this.readerIndex);
        this.readerIndex += 4;
        return v;
    }

    @Override
    public long readUnsignedInt() {
        return (long)this.readInt() & 0xFFFFFFFFL;
    }

    @Override
    public long readLong() {
        this.checkReadableBytes(8);
        long v = this.getLong(this.readerIndex);
        this.readerIndex += 8;
        return v;
    }

    @Override
    public char readChar() {
        return (char)this.readShort();
    }

    @Override
    public float readFloat() {
        return Float.intBitsToFloat(this.readInt());
    }

    @Override
    public double readDouble() {
        return Double.longBitsToDouble(this.readLong());
    }

    @Override
    public ByteBuf readBytes(int length) {
        this.checkReadableBytes(length);
        if (length == 0) {
            return Unpooled.EMPTY_BUFFER;
        }
        ByteBuf buf = Unpooled.buffer(length, this.maxCapacity);
        buf.writeBytes(this, this.readerIndex, length);
        this.readerIndex += length;
        return buf;
    }

    @Override
    public ByteBuf readSlice(int length) {
        ByteBuf slice = this.slice(this.readerIndex, length);
        this.readerIndex += length;
        return slice;
    }

    @Override
    public ByteBuf readBytes(byte[] dst, int dstIndex, int length) {
        this.checkReadableBytes(length);
        this.getBytes(this.readerIndex, dst, dstIndex, length);
        this.readerIndex += length;
        return this;
    }

    @Override
    public ByteBuf readBytes(byte[] dst) {
        this.readBytes(dst, 0, dst.length);
        return this;
    }

    @Override
    public ByteBuf readBytes(ByteBuf dst) {
        this.readBytes(dst, dst.writableBytes());
        return this;
    }

    @Override
    public ByteBuf readBytes(ByteBuf dst, int length) {
        if (length > dst.writableBytes()) {
            throw new IndexOutOfBoundsException("Too many bytes to be read: Need " + length + ", maximum is " + dst.writableBytes());
        }
        this.readBytes(dst, dst.writerIndex(), length);
        dst.writerIndex(dst.writerIndex() + length);
        return this;
    }

    @Override
    public ByteBuf readBytes(ByteBuf dst, int dstIndex, int length) {
        this.checkReadableBytes(length);
        this.getBytes(this.readerIndex, dst, dstIndex, length);
        this.readerIndex += length;
        return this;
    }

    @Override
    public ByteBuf readBytes(ByteBuffer dst) {
        int length = dst.remaining();
        this.checkReadableBytes(length);
        this.getBytes(this.readerIndex, dst);
        this.readerIndex += length;
        return this;
    }

    @Override
    public int readBytes(GatheringByteChannel out, int length) throws IOException {
        this.checkReadableBytes(length);
        int readBytes = this.getBytes(this.readerIndex, out, length);
        this.readerIndex += readBytes;
        return readBytes;
    }

    @Override
    public ByteBuf readBytes(OutputStream out, int length) throws IOException {
        this.checkReadableBytes(length);
        this.getBytes(this.readerIndex, out, length);
        this.readerIndex += length;
        return this;
    }

    @Override
    public ByteBuf skipBytes(int length) {
        int newReaderIndex = this.readerIndex + length;
        if (newReaderIndex > this.writerIndex) {
            throw new IndexOutOfBoundsException("Readable bytes exceeded - Need " + newReaderIndex + ", maximum is " + this.writerIndex);
        }
        this.readerIndex = newReaderIndex;
        return this;
    }

    @Override
    public ByteBuf writeBoolean(boolean value) {
        this.writeByte(value ? 1 : 0);
        return this;
    }

    @Override
    public ByteBuf writeByte(int value) {
        this.ensureWritableBytes(1);
        this.setByte(this.writerIndex++, value);
        return this;
    }

    @Override
    public ByteBuf writeShort(int value) {
        this.ensureWritableBytes(2);
        this.setShort(this.writerIndex, value);
        this.writerIndex += 2;
        return this;
    }

    @Override
    public ByteBuf writeMedium(int value) {
        this.ensureWritableBytes(3);
        this.setMedium(this.writerIndex, value);
        this.writerIndex += 3;
        return this;
    }

    @Override
    public ByteBuf writeInt(int value) {
        this.ensureWritableBytes(4);
        this.setInt(this.writerIndex, value);
        this.writerIndex += 4;
        return this;
    }

    @Override
    public ByteBuf writeLong(long value) {
        this.ensureWritableBytes(8);
        this.setLong(this.writerIndex, value);
        this.writerIndex += 8;
        return this;
    }

    @Override
    public ByteBuf writeChar(int value) {
        this.writeShort(value);
        return this;
    }

    @Override
    public ByteBuf writeFloat(float value) {
        this.writeInt(Float.floatToRawIntBits(value));
        return this;
    }

    @Override
    public ByteBuf writeDouble(double value) {
        this.writeLong(Double.doubleToRawLongBits(value));
        return this;
    }

    @Override
    public ByteBuf writeBytes(byte[] src, int srcIndex, int length) {
        this.ensureWritableBytes(length);
        this.setBytes(this.writerIndex, src, srcIndex, length);
        this.writerIndex += length;
        return this;
    }

    @Override
    public ByteBuf writeBytes(byte[] src) {
        this.writeBytes(src, 0, src.length);
        return this;
    }

    @Override
    public ByteBuf writeBytes(ByteBuf src) {
        this.writeBytes(src, src.readableBytes());
        return this;
    }

    @Override
    public ByteBuf writeBytes(ByteBuf src, int length) {
        if (length > src.readableBytes()) {
            throw new IndexOutOfBoundsException("Too many bytes to write - Need " + length + ", maximum is " + src.readableBytes());
        }
        this.writeBytes(src, src.readerIndex(), length);
        src.readerIndex(src.readerIndex() + length);
        return this;
    }

    @Override
    public ByteBuf writeBytes(ByteBuf src, int srcIndex, int length) {
        this.ensureWritableBytes(length);
        this.setBytes(this.writerIndex, src, srcIndex, length);
        this.writerIndex += length;
        return this;
    }

    @Override
    public ByteBuf writeBytes(ByteBuffer src) {
        int length = src.remaining();
        this.ensureWritableBytes(length);
        this.setBytes(this.writerIndex, src);
        this.writerIndex += length;
        return this;
    }

    @Override
    public int writeBytes(InputStream in, int length) throws IOException {
        this.ensureWritableBytes(length);
        int writtenBytes = this.setBytes(this.writerIndex, in, length);
        if (writtenBytes > 0) {
            this.writerIndex += writtenBytes;
        }
        return writtenBytes;
    }

    @Override
    public int writeBytes(ScatteringByteChannel in, int length) throws IOException {
        this.ensureWritableBytes(length);
        int writtenBytes = this.setBytes(this.writerIndex, in, length);
        if (writtenBytes > 0) {
            this.writerIndex += writtenBytes;
        }
        return writtenBytes;
    }

    @Override
    public ByteBuf writeZero(int length) {
        int i;
        if (length == 0) {
            return this;
        }
        if (length < 0) {
            throw new IllegalArgumentException("length must be 0 or greater than 0.");
        }
        int nLong = length >>> 3;
        int nBytes = length & 7;
        for (i = nLong; i > 0; --i) {
            this.writeLong(0);
        }
        if (nBytes == 4) {
            this.writeInt(0);
        } else if (nBytes < 4) {
            for (i = nBytes; i > 0; --i) {
                this.writeByte(0);
            }
        } else {
            this.writeInt(0);
            for (i = nBytes - 4; i > 0; --i) {
                this.writeByte(0);
            }
        }
        return this;
    }

    @Override
    public ByteBuf copy() {
        return this.copy(this.readerIndex, this.readableBytes());
    }

    @Override
    public ByteBuf duplicate() {
        return new DuplicatedByteBuf(this);
    }

    @Override
    public ByteBuf slice() {
        return this.slice(this.readerIndex, this.readableBytes());
    }

    @Override
    public ByteBuf slice(int index, int length) {
        if (length == 0) {
            return Unpooled.EMPTY_BUFFER;
        }
        return new SlicedByteBuf(this, index, length);
    }

    @Override
    public ByteBuffer nioBuffer() {
        return this.nioBuffer(this.readerIndex, this.readableBytes());
    }

    @Override
    public ByteBuffer[] nioBuffers() {
        return this.nioBuffers(this.readerIndex, this.readableBytes());
    }

    @Override
    public String toString(Charset charset) {
        return this.toString(this.readerIndex, this.readableBytes(), charset);
    }

    @Override
    public String toString(int index, int length, Charset charset) {
        ByteBuffer nioBuffer;
        if (length == 0) {
            return "";
        }
        if (this.hasNioBuffer()) {
            nioBuffer = this.nioBuffer(index, length);
        } else {
            nioBuffer = ByteBuffer.allocate(length);
            this.getBytes(index, nioBuffer);
            nioBuffer.flip();
        }
        return ByteBufUtil.decodeString(nioBuffer, charset);
    }

    @Override
    public int indexOf(int fromIndex, int toIndex, byte value) {
        return ByteBufUtil.indexOf((ByteBuf)this, fromIndex, toIndex, value);
    }

    @Override
    public int indexOf(int fromIndex, int toIndex, ByteBufIndexFinder indexFinder) {
        return ByteBufUtil.indexOf((ByteBuf)this, fromIndex, toIndex, indexFinder);
    }

    @Override
    public int bytesBefore(byte value) {
        return this.bytesBefore(this.readerIndex(), this.readableBytes(), value);
    }

    @Override
    public int bytesBefore(ByteBufIndexFinder indexFinder) {
        return this.bytesBefore(this.readerIndex(), this.readableBytes(), indexFinder);
    }

    @Override
    public int bytesBefore(int length, byte value) {
        this.checkReadableBytes(length);
        return this.bytesBefore(this.readerIndex(), length, value);
    }

    @Override
    public int bytesBefore(int length, ByteBufIndexFinder indexFinder) {
        this.checkReadableBytes(length);
        return this.bytesBefore(this.readerIndex(), length, indexFinder);
    }

    @Override
    public int bytesBefore(int index, int length, byte value) {
        int endIndex = this.indexOf(index, index + length, value);
        if (endIndex < 0) {
            return -1;
        }
        return endIndex - index;
    }

    @Override
    public int bytesBefore(int index, int length, ByteBufIndexFinder indexFinder) {
        int endIndex = this.indexOf(index, index + length, indexFinder);
        if (endIndex < 0) {
            return -1;
        }
        return endIndex - index;
    }

    @Override
    public int hashCode() {
        return ByteBufUtil.hashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof ByteBuf) {
            return ByteBufUtil.equals(this, (ByteBuf)o);
        }
        return false;
    }

    @Override
    public int compareTo(ByteBuf that) {
        return ByteBufUtil.compare(this, that);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '(' + "ridx=" + this.readerIndex + ", " + "widx=" + this.writerIndex + ", " + "cap=" + this.capacity() + ')';
    }

    protected void checkReadableBytes(int minimumReadableBytes) {
        if (this.readableBytes() < minimumReadableBytes) {
            throw new IndexOutOfBoundsException("Not enough readable bytes - Need " + minimumReadableBytes + ", maximum is " + this.readableBytes());
        }
    }
}

