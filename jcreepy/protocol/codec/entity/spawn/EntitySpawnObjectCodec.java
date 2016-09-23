
package jcreepy.protocol.codec.entity.spawn;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.entity.spawn.EntityObjectPacket;

public final class EntitySpawnObjectCodec
extends PacketCodec<EntityObjectPacket> {
    public EntitySpawnObjectCodec() {
        super(EntityObjectPacket.class, 23);
    }

    @Override
    public EntityObjectPacket decode(ByteBuf buffer) throws IOException {
        int entityId = buffer.readInt();
        byte type = buffer.readByte();
        int x = buffer.readInt();
        int y = buffer.readInt();
        int z = buffer.readInt();
        int throwerId = buffer.readInt();
        if (throwerId > 0) {
            short speedX = buffer.readShort();
            short speedY = buffer.readShort();
            short speedZ = buffer.readShort();
            return new EntityObjectPacket(entityId, type, x, y, z, throwerId, speedX, speedY, speedZ);
        }
        return new EntityObjectPacket(entityId, type, x, y, z, throwerId);
    }

    @Override
    public ByteBuf encode(EntityObjectPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(packet.getThrowerId() > 0 ? 27 : 21);
        buffer.writeInt(packet.getEntityId());
        buffer.writeByte(packet.getType());
        buffer.writeInt(packet.getX());
        buffer.writeInt(packet.getY());
        buffer.writeInt(packet.getZ());
        int throwerId = packet.getThrowerId();
        buffer.writeInt(throwerId);
        if (throwerId > 0) {
            buffer.writeShort(packet.getSpeedX());
            buffer.writeShort(packet.getSpeedY());
            buffer.writeShort(packet.getSpeedZ());
        }
        return buffer;
    }
}

