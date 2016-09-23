
package jcreepy.protocol.codec.entity.pos;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.entity.pos.EntityYawPacket;

public final class EntityYawCodec
extends PacketCodec<EntityYawPacket> {
    public EntityYawCodec() {
        super(EntityYawPacket.class, 32);
    }

    @Override
    public EntityYawPacket decode(ByteBuf buffer) throws IOException {
        int id = buffer.readInt();
        short rotation = buffer.readUnsignedByte();
        short pitch = buffer.readUnsignedByte();
        return new EntityYawPacket(id, rotation, pitch);
    }

    @Override
    public ByteBuf encode(EntityYawPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(6);
        buffer.writeInt(packet.getEntityId());
        buffer.writeByte(packet.getRotation());
        buffer.writeByte(packet.getPitch());
        return buffer;
    }
}

