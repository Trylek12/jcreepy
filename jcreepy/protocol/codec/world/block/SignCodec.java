
package jcreepy.protocol.codec.world.block;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.world.block.SignPacket;
import jcreepy.protocol.util.ByteBufUtils;

public final class SignCodec
extends PacketCodec<SignPacket> {
    public SignCodec() {
        super(SignPacket.class, 130);
    }

    @Override
    public SignPacket decode(ByteBuf buffer) throws IOException {
        int x = buffer.readInt();
        short y = buffer.readShort();
        int z = buffer.readInt();
        String[] message = new String[4];
        for (int i = 0; i < message.length; ++i) {
            String line = ByteBufUtils.readString(buffer);
            if (line == null) {
                line = "";
            }
            message[i] = line;
        }
        return new SignPacket(x, y, z, message);
    }

    @Override
    public ByteBuf encode(SignPacket packet) throws IOException {
        String[] lines = packet.getMessage();
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(packet.getX());
        buffer.writeShort(packet.getY());
        buffer.writeInt(packet.getZ());
        for (String line : lines) {
            if (line == null) {
                line = "";
            }
            ByteBufUtils.writeString(buffer, line);
        }
        return buffer;
    }
}

