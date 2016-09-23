
package jcreepy.protocol.codec.entity;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.entity.EntityAttachEntityPacket;

public final class EntityAttachEntityCodec
extends PacketCodec<EntityAttachEntityPacket> {
    public EntityAttachEntityCodec() {
        super(EntityAttachEntityPacket.class, 39);
    }

    @Override
    public EntityAttachEntityPacket decode(ByteBuf buffer) throws IOException {
        int id = buffer.readInt();
        int vehicle = buffer.readInt();
        return new EntityAttachEntityPacket(id, vehicle);
    }

    @Override
    public ByteBuf encode(EntityAttachEntityPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(8);
        buffer.writeInt(packet.getEntityId());
        buffer.writeInt(packet.getVehicle());
        return buffer;
    }
}

