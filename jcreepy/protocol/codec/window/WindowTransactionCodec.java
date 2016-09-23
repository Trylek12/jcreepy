
package jcreepy.protocol.codec.window;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.window.WindowTransactionPacket;

public final class WindowTransactionCodec
extends PacketCodec<WindowTransactionPacket> {
    public WindowTransactionCodec() {
        super(WindowTransactionPacket.class, 106);
    }

    @Override
    public WindowTransactionPacket decode(ByteBuf buffer) throws IOException {
        short id = buffer.readUnsignedByte();
        int transaction = buffer.readUnsignedShort();
        boolean accepted = buffer.readUnsignedByte() != 0;
        return new WindowTransactionPacket(id, transaction, accepted);
    }

    @Override
    public ByteBuf encode(WindowTransactionPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(4);
        buffer.writeByte(packet.getWindowInstanceId());
        buffer.writeShort(packet.getTransaction());
        buffer.writeByte(packet.isAccepted() ? 1 : 0);
        return buffer;
    }
}

