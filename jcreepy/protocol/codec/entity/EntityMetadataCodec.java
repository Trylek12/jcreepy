
package jcreepy.protocol.codec.entity;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.List;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.entity.EntityMetadataPacket;
import jcreepy.protocol.util.ByteBufUtils;
import jcreepy.protocol.util.Parameter;

public final class EntityMetadataCodec
extends PacketCodec<EntityMetadataPacket> {
    public EntityMetadataCodec() {
        super(EntityMetadataPacket.class, 40);
    }

    @Override
    public EntityMetadataPacket decode(ByteBuf buffer) throws IOException {
        int id = buffer.readInt();
        List parameters = ByteBufUtils.readParameters(buffer);
        return new EntityMetadataPacket(id, parameters);
    }

    @Override
    public ByteBuf encode(EntityMetadataPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(packet.getEntityId());
        ByteBufUtils.writeParameters(buffer, packet.getParameters());
        return buffer;
    }
}

