
package jcreepy.protocol.codec.entity.pos;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.entity.pos.EntityTeleportPacket;

public final class EntityTeleportCodec
extends PacketCodec<EntityTeleportPacket> {
    public EntityTeleportCodec() {
        super(EntityTeleportPacket.class, 34);
    }

    @Override
    public EntityTeleportPacket decode(ByteBuf buffer) throws IOException {
        int id = buffer.readInt();
        int x = buffer.readInt();
        int y = buffer.readInt();
        int z = buffer.readInt();
        short rotation = buffer.readUnsignedByte();
        short pitch = buffer.readUnsignedByte();
        return new EntityTeleportPacket(id, x, y, z, rotation, pitch);
    }

    @Override
    public ByteBuf encode(EntityTeleportPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(18);
        buffer.writeInt(packet.getEntityId());
        buffer.writeInt(packet.getX());
        buffer.writeInt(packet.getY());
        buffer.writeInt(packet.getZ());
        buffer.writeByte(packet.getRotation());
        buffer.writeByte(packet.getPitch());
        return buffer;
    }
}

