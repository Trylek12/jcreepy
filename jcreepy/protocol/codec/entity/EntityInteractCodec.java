
package jcreepy.protocol.codec.entity;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.entity.EntityInteractPacket;

public final class EntityInteractCodec
extends PacketCodec<EntityInteractPacket> {
    public EntityInteractCodec() {
        super(EntityInteractPacket.class, 7);
    }

    @Override
    public EntityInteractPacket decode(ByteBuf buffer) throws IOException {
        int id = buffer.readInt();
        int target = buffer.readInt();
        boolean punching = buffer.readByte() != 0;
        return new EntityInteractPacket(id, target, punching);
    }

    @Override
    public ByteBuf encode(EntityInteractPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(9);
        buffer.writeInt(packet.getEntityId());
        buffer.writeInt(packet.getTarget());
        buffer.writeByte(packet.isPunching() ? 1 : 0);
        return buffer;
    }
}

