
package jcreepy.protocol.codec.player;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.PlayerDiggingPacket;

public final class PlayerDiggingCodec
extends PacketCodec<PlayerDiggingPacket> {
    public PlayerDiggingCodec() {
        super(PlayerDiggingPacket.class, 14);
    }

    @Override
    public PlayerDiggingPacket decode(ByteBuf buffer) throws IOException {
        short state = buffer.readUnsignedByte();
        int x = buffer.readInt();
        short y = buffer.readUnsignedByte();
        int z = buffer.readInt();
        short blockFace = buffer.readUnsignedByte();
        return new PlayerDiggingPacket(state, x, y, z, blockFace);
    }

    @Override
    public ByteBuf encode(PlayerDiggingPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(11);
        buffer.writeByte(packet.getState());
        buffer.writeInt(packet.getX());
        buffer.writeByte(packet.getY());
        buffer.writeInt(packet.getZ());
        buffer.writeByte(packet.getFace());
        return buffer;
    }
}

