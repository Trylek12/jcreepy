
package jcreepy.protocol.codec.entity;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.entity.EntityStatusPacket;

public final class EntityStatusCodec
extends PacketCodec<EntityStatusPacket> {
    public EntityStatusCodec() {
        super(EntityStatusPacket.class, 38);
    }

    @Override
    public EntityStatusPacket decode(ByteBuf buffer) throws IOException {
        int id = buffer.readInt();
        byte status = buffer.readByte();
        return new EntityStatusPacket(id, status);
    }

    @Override
    public ByteBuf encode(EntityStatusPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(5);
        buffer.writeInt(packet.getEntityId());
        buffer.writeByte(packet.getStatus());
        return buffer;
    }
}

