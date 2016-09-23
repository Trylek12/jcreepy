
package jcreepy.network.codec;

import io.netty.buffer.ByteBuf;
import java.io.IOException;
import jcreepy.network.Packet;

public abstract class PacketCodec<T extends Packet> {
    private final Class<T> clazz;
    private int opcode;

    public PacketCodec(Class<T> clazz, int opcode) {
        this.clazz = clazz;
        this.opcode = opcode;
    }

    public final Class<T> getType() {
        return this.clazz;
    }

    public final int getOpcode() {
        return this.opcode;
    }

    void setOpcode(int opcode) {
        this.opcode = opcode;
    }

    public ByteBuf encode(T packet) throws IOException {
        return null;
    }

    public T decode(ByteBuf buffer) throws IOException {
        return null;
    }
}

