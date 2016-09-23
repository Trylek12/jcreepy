
package jcreepy.protocol.codec.entity;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.entity.EntityActionPacket;

public final class EntityActionCodec
extends PacketCodec<EntityActionPacket> {
    public EntityActionCodec() {
        super(EntityActionPacket.class, 19);
    }

    @Override
    public EntityActionPacket decode(ByteBuf buffer) throws IOException {
        int id = buffer.readInt();
        short action = buffer.readUnsignedByte();
        return new EntityActionPacket(id, action);
    }

    @Override
    public ByteBuf encode(EntityActionPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(5);
        buffer.writeInt(packet.getEntityId());
        buffer.writeByte(packet.getAction());
        return buffer;
    }
}

