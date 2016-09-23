
package jcreepy.protocol.codec.entity;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.entity.EntityInitializePacket;

public final class EntityInitializeCodec
extends PacketCodec<EntityInitializePacket> {
    public EntityInitializeCodec() {
        super(EntityInitializePacket.class, 30);
    }

    @Override
    public EntityInitializePacket decode(ByteBuf buffer) throws IOException {
        int id = buffer.readInt();
        return new EntityInitializePacket(id);
    }

    @Override
    public ByteBuf encode(EntityInitializePacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(4);
        buffer.writeInt(packet.getEntityId());
        return buffer;
    }
}

