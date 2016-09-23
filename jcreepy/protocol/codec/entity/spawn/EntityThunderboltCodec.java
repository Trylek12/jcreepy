
package jcreepy.protocol.codec.entity.spawn;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.entity.spawn.EntityThunderboltPacket;

public final class EntityThunderboltCodec
extends PacketCodec<EntityThunderboltPacket> {
    public EntityThunderboltCodec() {
        super(EntityThunderboltPacket.class, 71);
    }

    @Override
    public EntityThunderboltPacket decode(ByteBuf buffer) throws IOException {
        int id = buffer.readInt();
        short mode = buffer.readUnsignedByte();
        int x = buffer.readInt();
        int y = buffer.readInt();
        int z = buffer.readInt();
        return new EntityThunderboltPacket(id, mode, x, y, z);
    }

    @Override
    public ByteBuf encode(EntityThunderboltPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(17);
        buffer.writeInt(packet.getEntityId());
        buffer.writeByte(packet.getMode());
        buffer.writeInt(packet.getX());
        buffer.writeInt(packet.getY());
        buffer.writeInt(packet.getZ());
        return buffer;
    }
}

