
package jcreepy.protocol.codec.entity.spawn;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.entity.spawn.EntityExperienceOrbPacket;

public class EntityExperienceOrbCodec
extends PacketCodec<EntityExperienceOrbPacket> {
    public EntityExperienceOrbCodec() {
        super(EntityExperienceOrbPacket.class, 26);
    }

    @Override
    public EntityExperienceOrbPacket decode(ByteBuf buffer) throws IOException {
        int id = buffer.readInt();
        int x = buffer.readInt();
        int y = buffer.readInt();
        int z = buffer.readInt();
        short count = buffer.readShort();
        return new EntityExperienceOrbPacket(id, x, y, z, count);
    }

    @Override
    public ByteBuf encode(EntityExperienceOrbPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(18);
        buffer.writeInt(packet.getEntityId());
        buffer.writeInt(packet.getX());
        buffer.writeInt(packet.getY());
        buffer.writeInt(packet.getZ());
        buffer.writeShort(packet.getCount());
        return buffer;
    }
}

