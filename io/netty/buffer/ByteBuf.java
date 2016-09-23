
package io.netty.buffer;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufIndexFinder;
import io.netty.buffer.ChannelBuf;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;

public interface ByteBuf
extends ChannelBuf,
Comparable<ByteBuf> {
    public int capacity();

    public ByteBuf capacity(int var1);

    public int maxCapacity();

    public ByteBufAllocator alloc();

    public ByteOrder order();

    public ByteBuf order(ByteOrder var1);

    public ByteBuf unwrap();

    public boolean isDirect();

    public int readerIndex();

    public ByteBuf readerIndex(int var1);

    public int writerIndex();

    public ByteBuf writerIndex(int var1);

    public ByteBuf setIndex(int var1, int var2);

    public int readableBytes();

    public int writableBytes();

    public boolean readable();

    public boolean writable();

    public ByteBuf clear();

    public ByteBuf markReaderIndex();

    public ByteBuf resetReaderIndex();

    public ByteBuf markWriterIndex();

    public ByteBuf resetWriterIndex();

    public ByteBuf discardReadBytes();

    public ByteBuf ensureWritableBytes(int var1);

    public int ensureWritableBytes(int var1, boolean var2);

    public boolean getBoolean(int var1);

    public byte getByte(int var1);

    public short getUnsignedByte(int var1);

    public short getShort(int var1);

    public int getUnsignedShort(int var1);

    public int getMedium(int var1);

    public int getUnsignedMedium(int var1);

    public int getInt(int var1);

    public long getUnsignedInt(int var1);

    public long getLong(int var1);

    public char getChar(int var1);

    public float getFloat(int var1);

    public double getDouble(int var1);

    public ByteBuf getBytes(int var1, ByteBuf var2);

    public ByteBuf getBytes(int var1, ByteBuf var2, int var3);

    public ByteBuf getBytes(int var1, ByteBuf var2, int var3, int var4);

    public ByteBuf getBytes(int var1, byte[] var2);

    public ByteBuf getBytes(int var1, byte[] var2, int var3, int var4);

    public ByteBuf getBytes(int var1, ByteBuffer var2);

    public ByteBuf getBytes(int var1, OutputStream var2, int var3) throws IOException;

    public int getBytes(int var1, GatheringByteChannel var2, int var3) throws IOException;

    public ByteBuf setBoolean(int var1, boolean var2);

    public ByteBuf setByte(int var1, int var2);

    public ByteBuf setShort(int var1, int var2);

    public ByteBuf setMedium(int var1, int var2);

    public ByteBuf setInt(int var1, int var2);

    public ByteBuf setLong(int var1, long var2);

    public ByteBuf setChar(int var1, int var2);

    public ByteBuf setFloat(int var1, float var2);

    public ByteBuf setDouble(int var1, double var2);

    public ByteBuf setBytes(int var1, ByteBuf var2);

    public ByteBuf setBytes(int var1, ByteBuf var2, int var3);

    public ByteBuf setBytes(int var1, ByteBuf var2, int var3, int var4);

    public ByteBuf setBytes(int var1, byte[] var2);

    public ByteBuf setBytes(int var1, byte[] var2, int var3, int var4);

    public ByteBuf setBytes(int var1, ByteBuffer var2);

    public int setBytes(int var1, InputStream var2, int var3) throws IOException;

    public int setBytes(int var1, ScatteringByteChannel var2, int var3) throws IOException;

    public ByteBuf setZero(int var1, int var2);

    public boolean readBoolean();

    public byte readByte();

    public short readUnsignedByte();

    public short readShort();

    public int readUnsignedShort();

    public int readMedium();

    public int readUnsignedMedium();

    public int readInt();

    public long readUnsignedInt();

    public long readLong();

    public char readChar();

    public float readFloat();

    public double readDouble();

    public ByteBuf readBytes(int var1);

    public ByteBuf readSlice(int var1);

    public ByteBuf readBytes(ByteBuf var1);

    public ByteBuf readBytes(ByteBuf var1, int var2);

    public ByteBuf readBytes(ByteBuf var1, int var2, int var3);

    public ByteBuf readBytes(byte[] var1);

    public ByteBuf readBytes(byte[] var1, int var2, int var3);

    public ByteBuf readBytes(ByteBuffer var1);

    public ByteBuf readBytes(OutputStream var1, int var2) throws IOException;

    public int readBytes(GatheringByteChannel var1, int var2) throws IOException;

    public ByteBuf skipBytes(int var1);

    public ByteBuf writeBoolean(boolean var1);

    public ByteBuf writeByte(int var1);

    public ByteBuf writeShort(int var1);

    public ByteBuf writeMedium(int var1);

    public ByteBuf writeInt(int var1);

    public ByteBuf writeLong(long var1);

    public ByteBuf writeChar(int var1);

    public ByteBuf writeFloat(float var1);

    public ByteBuf writeDouble(double var1);

    public ByteBuf writeBytes(ByteBuf var1);

    public ByteBuf writeBytes(ByteBuf var1, int var2);

    public ByteBuf writeBytes(ByteBuf var1, int var2, int var3);

    public ByteBuf writeBytes(byte[] var1);

    public ByteBuf writeBytes(byte[] var1, int var2, int var3);

    public ByteBuf writeBytes(ByteBuffer var1);

    public int writeBytes(InputStream var1, int var2) throws IOException;

    public int writeBytes(ScatteringByteChannel var1, int var2) throws IOException;

    public ByteBuf writeZero(int var1);

    public int indexOf(int var1, int var2, byte var3);

    public int indexOf(int var1, int var2, ByteBufIndexFinder var3);

    public int bytesBefore(byte var1);

    public int bytesBefore(ByteBufIndexFinder var1);

    public int bytesBefore(int var1, byte var2);

    public int bytesBefore(int var1, ByteBufIndexFinder var2);

    public int bytesBefore(int var1, int var2, byte var3);

    public int bytesBefore(int var1, int var2, ByteBufIndexFinder var3);

    public ByteBuf copy();

    public ByteBuf copy(int var1, int var2);

    public ByteBuf slice();

    public ByteBuf slice(int var1, int var2);

    public ByteBuf duplicate();

    public boolean hasNioBuffer();

    public ByteBuffer nioBuffer();

    public ByteBuffer nioBuffer(int var1, int var2);

    public boolean hasNioBuffers();

    public ByteBuffer[] nioBuffers();

    public ByteBuffer[] nioBuffers(int var1, int var2);

    public boolean hasArray();

    public byte[] array();

    public int arrayOffset();

    public String toString(Charset var1);

    public String toString(int var1, int var2, Charset var3);

    public int hashCode();

    public boolean equals(Object var1);

    @Override
    public int compareTo(ByteBuf var1);

    public String toString();

    @Override
    public Unsafe unsafe();

    public static interface Unsafe
    extends ChannelBuf.Unsafe {
        public ByteBuffer internalNioBuffer();

        public ByteBuffer[] internalNioBuffers();

        public void discardSomeReadBytes();

        public void suspendIntermediaryDeallocations();

        public void resumeIntermediaryDeallocations();
    }

}

