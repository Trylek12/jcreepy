
package io.netty.buffer;

import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;

public interface CompositeByteBuf
extends ByteBuf,
Iterable<ByteBuf> {
    public CompositeByteBuf addComponent(ByteBuf var1);

    public CompositeByteBuf addComponent(int var1, ByteBuf var2);

    public /* varargs */ CompositeByteBuf addComponents(ByteBuf ... var1);

    public CompositeByteBuf addComponents(Iterable<ByteBuf> var1);

    public /* varargs */ CompositeByteBuf addComponents(int var1, ByteBuf ... var2);

    public CompositeByteBuf addComponents(int var1, Iterable<ByteBuf> var2);

    public CompositeByteBuf removeComponent(int var1);

    public CompositeByteBuf removeComponents(int var1, int var2);

    public int numComponents();

    public int maxNumComponents();

    public ByteBuf component(int var1);

    public ByteBuf componentAtOffset(int var1);

    public CompositeByteBuf discardReadComponents();

    public CompositeByteBuf consolidate();

    public CompositeByteBuf consolidate(int var1, int var2);

    public int toComponentIndex(int var1);

    public int toByteIndex(int var1);

    public List<ByteBuf> decompose(int var1, int var2);

    @Override
    public CompositeByteBuf capacity(int var1);

    @Override
    public CompositeByteBuf readerIndex(int var1);

    @Override
    public CompositeByteBuf writerIndex(int var1);

    @Override
    public CompositeByteBuf setIndex(int var1, int var2);

    @Override
    public CompositeByteBuf clear();

    @Override
    public CompositeByteBuf markReaderIndex();

    @Override
    public CompositeByteBuf resetReaderIndex();

    @Override
    public CompositeByteBuf markWriterIndex();

    @Override
    public CompositeByteBuf resetWriterIndex();

    @Override
    public CompositeByteBuf discardReadBytes();

    @Override
    public CompositeByteBuf ensureWritableBytes(int var1);

    @Override
    public CompositeByteBuf getBytes(int var1, ByteBuf var2);

    @Override
    public CompositeByteBuf getBytes(int var1, ByteBuf var2, int var3);

    @Override
    public CompositeByteBuf getBytes(int var1, ByteBuf var2, int var3, int var4);

    @Override
    public CompositeByteBuf getBytes(int var1, byte[] var2);

    @Override
    public CompositeByteBuf getBytes(int var1, byte[] var2, int var3, int var4);

    @Override
    public CompositeByteBuf getBytes(int var1, ByteBuffer var2);

    @Override
    public CompositeByteBuf getBytes(int var1, OutputStream var2, int var3) throws IOException;

    @Override
    public CompositeByteBuf setBoolean(int var1, boolean var2);

    @Override
    public CompositeByteBuf setByte(int var1, int var2);

    @Override
    public CompositeByteBuf setShort(int var1, int var2);

    @Override
    public CompositeByteBuf setMedium(int var1, int var2);

    @Override
    public CompositeByteBuf setInt(int var1, int var2);

    @Override
    public CompositeByteBuf setLong(int var1, long var2);

    @Override
    public CompositeByteBuf setChar(int var1, int var2);

    @Override
    public CompositeByteBuf setFloat(int var1, float var2);

    @Override
    public CompositeByteBuf setDouble(int var1, double var2);

    @Override
    public CompositeByteBuf setBytes(int var1, ByteBuf var2);

    @Override
    public CompositeByteBuf setBytes(int var1, ByteBuf var2, int var3);

    @Override
    public CompositeByteBuf setBytes(int var1, ByteBuf var2, int var3, int var4);

    @Override
    public CompositeByteBuf setBytes(int var1, byte[] var2);

    @Override
    public CompositeByteBuf setBytes(int var1, byte[] var2, int var3, int var4);

    @Override
    public CompositeByteBuf setBytes(int var1, ByteBuffer var2);

    @Override
    public CompositeByteBuf setZero(int var1, int var2);

    @Override
    public CompositeByteBuf readBytes(ByteBuf var1);

    @Override
    public CompositeByteBuf readBytes(ByteBuf var1, int var2);

    @Override
    public CompositeByteBuf readBytes(ByteBuf var1, int var2, int var3);

    @Override
    public CompositeByteBuf readBytes(byte[] var1);

    @Override
    public CompositeByteBuf readBytes(byte[] var1, int var2, int var3);

    @Override
    public CompositeByteBuf readBytes(ByteBuffer var1);

    @Override
    public CompositeByteBuf readBytes(OutputStream var1, int var2) throws IOException;

    @Override
    public CompositeByteBuf skipBytes(int var1);

    @Override
    public CompositeByteBuf writeBoolean(boolean var1);

    @Override
    public CompositeByteBuf writeByte(int var1);

    @Override
    public CompositeByteBuf writeShort(int var1);

    @Override
    public CompositeByteBuf writeMedium(int var1);

    @Override
    public CompositeByteBuf writeInt(int var1);

    @Override
    public CompositeByteBuf writeLong(long var1);

    @Override
    public CompositeByteBuf writeChar(int var1);

    @Override
    public CompositeByteBuf writeFloat(float var1);

    @Override
    public CompositeByteBuf writeDouble(double var1);

    @Override
    public CompositeByteBuf writeBytes(ByteBuf var1);

    @Override
    public CompositeByteBuf writeBytes(ByteBuf var1, int var2);

    @Override
    public CompositeByteBuf writeBytes(ByteBuf var1, int var2, int var3);

    @Override
    public CompositeByteBuf writeBytes(byte[] var1);

    @Override
    public CompositeByteBuf writeBytes(byte[] var1, int var2, int var3);

    @Override
    public CompositeByteBuf writeBytes(ByteBuffer var1);

    @Override
    public CompositeByteBuf writeZero(int var1);
}

