
package jcreepy.protocol.codec.entity.pos;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.entity.pos.EntityHeadYawPacket;

public class EntityHeadYawCodec
extends PacketCodec<EntityHeadYawPacket> {
    public EntityHeadYawCodec() {
        super(EntityHeadYawPacket.class, 35);
    }

    @Override
    public EntityHeadYawPacket decode(ByteBuf buffer) throws IOException {
        int id = buffer.readInt();
        short headYaw = buffer.readUnsignedByte();
        return new EntityHeadYawPacket(id, headYaw);
    }

    @Override
    public ByteBuf encode(EntityHeadYawPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(5);
        buffer.writeInt(packet.getEntityId());
        buffer.writeByte(packet.getHeadYaw());
        return buffer;
    }
}

