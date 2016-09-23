
package jcreepy.protocol.codec.entity.pos;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.entity.pos.EntityVelocityPacket;

public final class EntityVelocityCodec
extends PacketCodec<EntityVelocityPacket> {
    public EntityVelocityCodec() {
        super(EntityVelocityPacket.class, 28);
    }

    @Override
    public EntityVelocityPacket decode(ByteBuf buffer) throws IOException {
        int id = buffer.readInt();
        int vx = buffer.readUnsignedShort();
        int vy = buffer.readUnsignedShort();
        int vz = buffer.readUnsignedShort();
        return new EntityVelocityPacket(id, vx, vy, vz);
    }

    @Override
    public ByteBuf encode(EntityVelocityPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(10);
        buffer.writeInt(packet.getEntityId());
        buffer.writeShort(packet.getVelocityX());
        buffer.writeShort(packet.getVelocityY());
        buffer.writeShort(packet.getVelocityZ());
        return buffer;
    }
}

