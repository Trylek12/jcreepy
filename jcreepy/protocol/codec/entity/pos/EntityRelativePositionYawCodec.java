
package jcreepy.protocol.codec.entity.pos;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.entity.pos.EntityRelativePositionYawPacket;

public final class EntityRelativePositionYawCodec
extends PacketCodec<EntityRelativePositionYawPacket> {
    public EntityRelativePositionYawCodec() {
        super(EntityRelativePositionYawPacket.class, 33);
    }

    @Override
    public EntityRelativePositionYawPacket decode(ByteBuf buffer) throws IOException {
        int id = buffer.readInt();
        byte dx = buffer.readByte();
        byte dy = buffer.readByte();
        byte dz = buffer.readByte();
        short rotation = buffer.readUnsignedByte();
        short pitch = buffer.readUnsignedByte();
        return new EntityRelativePositionYawPacket(id, dx, dy, dz, rotation, pitch);
    }

    @Override
    public ByteBuf encode(EntityRelativePositionYawPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(9);
        buffer.writeInt(packet.getEntityId());
        buffer.writeByte(packet.getDeltaX());
        buffer.writeByte(packet.getDeltaY());
        buffer.writeByte(packet.getDeltaZ());
        buffer.writeByte(packet.getRotation());
        buffer.writeByte(packet.getPitch());
        return buffer;
    }
}

