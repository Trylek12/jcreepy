
package jcreepy.nbt.stream;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

public final class EndianSwitchableInputStream
extends FilterInputStream
implements DataInput {
    private final ByteOrder endianness;

    public EndianSwitchableInputStream(InputStream stream, ByteOrder endianness) {
        super(stream instanceof DataInputStream ? stream : new DataInputStream(stream));
        this.endianness = endianness;
    }

    public ByteOrder getEndianness() {
        return this.endianness;
    }

    protected DataInputStream getBackingStream() {
        return (DataInputStream)this.in;
    }

    @Override
    public void readFully(byte[] bytes) throws IOException {
        this.getBackingStream().readFully(bytes);
    }

    @Override
    public void readFully(byte[] bytes, int i, int i1) throws IOException {
        this.getBackingStream().readFully(bytes, i, i1);
    }

    @Override
    public int skipBytes(int i) throws IOException {
        return this.getBackingStream().skipBytes(i);
    }

    @Override
    public boolean readBoolean() throws IOException {
        return this.getBackingStream().readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return this.getBackingStream().readByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return this.getBackingStream().readUnsignedByte();
    }

    @Override
    public short readShort() throws IOException {
        short ret = this.getBackingStream().readShort();
        if (this.endianness == ByteOrder.LITTLE_ENDIAN) {
            ret = Short.reverseBytes(ret);
        }
        return ret;
    }

    @Override
    public int readUnsignedShort() throws IOException {
        int ret = this.getBackingStream().readUnsignedShort();
        if (this.endianness == ByteOrder.LITTLE_ENDIAN) {
            ret = (char)(Integer.reverseBytes(ret) >> 16);
        }
        return ret;
    }

    @Override
    public char readChar() throws IOException {
        char ret = this.getBackingStream().readChar();
        if (this.endianness == ByteOrder.LITTLE_ENDIAN) {
            ret = Character.reverseBytes(ret);
        }
        return ret;
    }

    @Override
    public int readInt() throws IOException {
        return this.endianness == ByteOrder.LITTLE_ENDIAN ? Integer.reverseBytes(this.getBackingStream().readInt()) : this.getBackingStream().readInt();
    }

    @Override
    public long readLong() throws IOException {
        return this.endianness == ByteOrder.LITTLE_ENDIAN ? Long.reverseBytes(this.getBackingStream().readLong()) : this.getBackingStream().readLong();
    }

    @Override
    public float readFloat() throws IOException {
        int result = this.readInt();
        if (this.endianness == ByteOrder.LITTLE_ENDIAN) {
            result = Integer.reverseBytes(result);
        }
        return Float.intBitsToFloat(result);
    }

    @Override
    public double readDouble() throws IOException {
        long result = this.readLong();
        if (this.endianness == ByteOrder.LITTLE_ENDIAN) {
            result = Long.reverseBytes(result);
        }
        return Double.longBitsToDouble(result);
    }

    @Override
    public String readLine() throws IOException {
        return this.getBackingStream().readLine();
    }

    @Override
    public String readUTF() throws IOException {
        return this.getBackingStream().readUTF();
    }
}

