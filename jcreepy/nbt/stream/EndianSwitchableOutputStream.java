
package jcreepy.nbt.stream;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;

public final class EndianSwitchableOutputStream
extends FilterOutputStream
implements DataOutput {
    private final ByteOrder endianness;

    public EndianSwitchableOutputStream(OutputStream backingStream, ByteOrder endianness) {
        super(backingStream instanceof DataOutputStream ? (DataOutputStream)backingStream : new DataOutputStream(backingStream));
        this.endianness = endianness;
    }

    public ByteOrder getEndianness() {
        return this.endianness;
    }

    protected DataOutputStream getBackingStream() {
        return (DataOutputStream)this.out;
    }

    @Override
    public void writeBoolean(boolean b) throws IOException {
        this.getBackingStream().writeBoolean(b);
    }

    @Override
    public void writeByte(int i) throws IOException {
        this.getBackingStream().writeByte(i);
    }

    @Override
    public void writeShort(int i) throws IOException {
        if (this.endianness == ByteOrder.LITTLE_ENDIAN) {
            i = Integer.reverseBytes(i) >> 16;
        }
        this.getBackingStream().writeShort(i);
    }

    @Override
    public void writeChar(int i) throws IOException {
        if (this.endianness == ByteOrder.LITTLE_ENDIAN) {
            i = Character.reverseBytes((char)i);
        }
        this.getBackingStream().writeChar(i);
    }

    @Override
    public void writeInt(int i) throws IOException {
        if (this.endianness == ByteOrder.LITTLE_ENDIAN) {
            i = Integer.reverseBytes(i);
        }
        this.getBackingStream().writeInt(i);
    }

    @Override
    public void writeLong(long l) throws IOException {
        if (this.endianness == ByteOrder.LITTLE_ENDIAN) {
            l = Long.reverseBytes(l);
        }
        this.getBackingStream().writeLong(l);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        int intBits = Float.floatToIntBits(v);
        if (this.endianness == ByteOrder.LITTLE_ENDIAN) {
            intBits = Integer.reverseBytes(intBits);
        }
        this.getBackingStream().writeInt(intBits);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        long longBits = Double.doubleToLongBits(v);
        if (this.endianness == ByteOrder.LITTLE_ENDIAN) {
            longBits = Long.reverseBytes(longBits);
        }
        this.getBackingStream().writeLong(longBits);
    }

    @Override
    public void writeBytes(String s) throws IOException {
        this.getBackingStream().writeBytes(s);
    }

    @Override
    public void writeChars(String s) throws IOException {
        this.getBackingStream().writeChars(s);
    }

    @Override
    public void writeUTF(String s) throws IOException {
        this.getBackingStream().writeUTF(s);
    }
}

