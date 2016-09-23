
package jcreepy.protocol.codec.entity;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.entity.pos.EntityRelativePositionPacket;

public final class EntityRelativePositionCodec
extends PacketCodec<EntityRelativePositionPacket> {
    public EntityRelativePositionCodec() {
        super(EntityRelativePositionPacket.class, 31);
    }

    @Override
    public EntityRelativePositionPacket decode(ByteBuf buffer) throws IOException {
        int id = buffer.readInt();
        byte dx = buffer.readByte();
        byte dy = buffer.readByte();
        byte dz = buffer.readByte();
        return new EntityRelativePositionPacket(id, dx, dy, dz);
    }

    @Override
    public ByteBuf encode(EntityRelativePositionPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(7);
        buffer.writeInt(packet.getEntityId());
        buffer.writeByte(packet.getDeltaX());
        buffer.writeByte(packet.getDeltaY());
        buffer.writeByte(packet.getDeltaZ());
        return buffer;
    }
}

