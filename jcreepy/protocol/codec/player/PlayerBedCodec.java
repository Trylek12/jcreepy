
package jcreepy.protocol.codec.player;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.PlayerBedPacket;

public final class PlayerBedCodec
extends PacketCodec<PlayerBedPacket> {
    public PlayerBedCodec() {
        super(PlayerBedPacket.class, 17);
    }

    @Override
    public PlayerBedPacket decode(ByteBuf buffer) throws IOException {
        int id = buffer.readInt();
        short used = buffer.readUnsignedByte();
        int x = buffer.readInt();
        short y = buffer.readUnsignedByte();
        int z = buffer.readInt();
        return new PlayerBedPacket(id, used, x, y, z);
    }

    @Override
    public ByteBuf encode(PlayerBedPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(14);
        buffer.writeInt(packet.getEntityId());
        buffer.writeByte(packet.getUsed());
        buffer.writeInt(packet.getX());
        buffer.writeByte(packet.getY());
        buffer.writeInt(packet.getZ());
        return buffer;
    }
}

