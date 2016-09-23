
package jcreepy.protocol.codec.player.pos;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.List;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.pos.PlayerSpawnPacket;
import jcreepy.protocol.util.ByteBufUtils;
import jcreepy.protocol.util.Parameter;

public final class PlayerSpawnCodec
extends PacketCodec<PlayerSpawnPacket> {
    public PlayerSpawnCodec() {
        super(PlayerSpawnPacket.class, 20);
    }

    @Override
    public PlayerSpawnPacket decode(ByteBuf buffer) throws IOException {
        int id = buffer.readInt();
        String name = ByteBufUtils.readString(buffer);
        int x = buffer.readInt();
        int y = buffer.readInt();
        int z = buffer.readInt();
        short rotation = buffer.readUnsignedByte();
        short pitch = buffer.readUnsignedByte();
        int item = buffer.readUnsignedShort();
        List parameters = ByteBufUtils.readParameters(buffer);
        return new PlayerSpawnPacket(id, name, x, y, z, rotation, pitch, item, parameters);
    }

    @Override
    public ByteBuf encode(PlayerSpawnPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(packet.getEntityId());
        ByteBufUtils.writeString(buffer, packet.getPlayerName());
        buffer.writeInt(packet.getX());
        buffer.writeInt(packet.getY());
        buffer.writeInt(packet.getZ());
        buffer.writeByte(packet.getYaw());
        buffer.writeByte(packet.getPitch());
        buffer.writeShort(packet.getId());
        ByteBufUtils.writeParameters(buffer, packet.getParameters());
        return buffer;
    }
}

