
package jcreepy.protocol.codec.entity.spawn;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.List;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.entity.spawn.EntityMobPacket;
import jcreepy.protocol.util.ByteBufUtils;
import jcreepy.protocol.util.Parameter;

public final class EntityMobCodec
extends PacketCodec<EntityMobPacket> {
    public EntityMobCodec() {
        super(EntityMobPacket.class, 24);
    }

    @Override
    public EntityMobPacket decode(ByteBuf buffer) throws IOException {
        int id = buffer.readInt();
        short type = buffer.readUnsignedByte();
        int x = buffer.readInt();
        int y = buffer.readInt();
        int z = buffer.readInt();
        short yaw = buffer.readUnsignedByte();
        short pitch = buffer.readUnsignedByte();
        short headYaw = buffer.readUnsignedByte();
        short velocityZ = buffer.readShort();
        short velocityX = buffer.readShort();
        short velocityY = buffer.readShort();
        List parameters = ByteBufUtils.readParameters(buffer);
        return new EntityMobPacket(id, type, x, y, z, yaw, pitch, headYaw, velocityZ, velocityX, velocityY, parameters);
    }

    @Override
    public ByteBuf encode(EntityMobPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(packet.getEntityId());
        buffer.writeByte(packet.getType());
        buffer.writeInt(packet.getX());
        buffer.writeInt(packet.getY());
        buffer.writeInt(packet.getZ());
        buffer.writeByte(packet.getYaw());
        buffer.writeByte(packet.getPitch());
        buffer.writeByte(packet.getHeadYaw());
        buffer.writeShort(packet.getVelocityZ());
        buffer.writeShort(packet.getVelocityX());
        buffer.writeShort(packet.getVelocityY());
        ByteBufUtils.writeParameters(buffer, packet.getParameters());
        return buffer;
    }
}

