
package jcreepy.protocol.codec.entity.spawn;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.entity.spawn.EntityPaintingPacket;
import jcreepy.protocol.util.ByteBufUtils;

public final class EntityPaintingCodec
extends PacketCodec<EntityPaintingPacket> {
    public EntityPaintingCodec() {
        super(EntityPaintingPacket.class, 25);
    }

    @Override
    public EntityPaintingPacket decode(ByteBuf buffer) throws IOException {
        int id = buffer.readInt();
        String title = ByteBufUtils.readString(buffer);
        int x = buffer.readInt();
        int y = buffer.readInt();
        int z = buffer.readInt();
        int direction = buffer.readInt();
        return new EntityPaintingPacket(id, title, x, y, z, direction);
    }

    @Override
    public ByteBuf encode(EntityPaintingPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(packet.getEntityId());
        ByteBufUtils.writeString(buffer, packet.getTitle());
        buffer.writeInt(packet.getX());
        buffer.writeInt(packet.getY());
        buffer.writeInt(packet.getZ());
        buffer.writeInt(packet.getDirection());
        return buffer;
    }
}

