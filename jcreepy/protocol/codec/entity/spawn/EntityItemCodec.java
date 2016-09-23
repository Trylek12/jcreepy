
package jcreepy.protocol.codec.entity.spawn;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.entity.spawn.EntityItemPacket;
import jcreepy.protocol.util.ByteBufUtils;

public final class EntityItemCodec
extends PacketCodec<EntityItemPacket> {
    public EntityItemCodec() {
        super(EntityItemPacket.class, 21);
    }

    @Override
    public EntityItemPacket decode(ByteBuf buffer) throws IOException {
        int id = buffer.readInt();
        int itemId = buffer.readUnsignedShort();
        short count = 1;
        int damage = 0;
        if (itemId != -1) {
            count = buffer.readUnsignedByte();
            damage = buffer.readUnsignedShort();
        }
        buffer.markReaderIndex();
        short strLen = buffer.readShort();
        if (strLen != -1) {
            buffer.resetReaderIndex();
            ByteBufUtils.readCompound(buffer);
        }
        int x = buffer.readInt();
        int y = buffer.readInt();
        int z = buffer.readInt();
        short rotation = buffer.readUnsignedByte();
        short pitch = buffer.readUnsignedByte();
        short roll = buffer.readUnsignedByte();
        return new EntityItemPacket(id, itemId, count, (short)damage, x, y, z, rotation, pitch, roll);
    }

    @Override
    public ByteBuf encode(EntityItemPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(packet.getEntityId());
        buffer.writeShort(packet.getId());
        buffer.writeByte(packet.getCount());
        buffer.writeShort(packet.getDamage());
        buffer.writeShort(-1);
        buffer.writeInt(packet.getX());
        buffer.writeInt(packet.getY());
        buffer.writeInt(packet.getZ());
        buffer.writeByte(packet.getRotation());
        buffer.writeByte(packet.getPitch());
        buffer.writeByte(packet.getRoll());
        return buffer;
    }
}

