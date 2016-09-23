
package jcreepy.protocol.codec.entity;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.entity.EntityAnimationPacket;

public final class EntityAnimationCodec
extends PacketCodec<EntityAnimationPacket> {
    public EntityAnimationCodec() {
        super(EntityAnimationPacket.class, 18);
    }

    @Override
    public EntityAnimationPacket decode(ByteBuf buffer) throws IOException {
        int id = buffer.readInt();
        byte animation = buffer.readByte();
        return new EntityAnimationPacket(id, animation);
    }

    @Override
    public ByteBuf encode(EntityAnimationPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(5);
        buffer.writeInt(packet.getEntityId());
        buffer.writeByte(packet.getAnimationId());
        return buffer;
    }
}

