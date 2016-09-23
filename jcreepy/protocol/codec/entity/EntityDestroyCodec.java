
package jcreepy.protocol.codec.entity;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.entity.EntityDestroyPacket;

public final class EntityDestroyCodec
extends PacketCodec<EntityDestroyPacket> {
    public EntityDestroyCodec() {
        super(EntityDestroyPacket.class, 29);
    }

    @Override
    public EntityDestroyPacket decode(ByteBuf buffer) throws IOException {
        int length = buffer.readByte();
        int[] entityid = new int[length];
        for (int i = 0; i < length; ++i) {
            entityid[i] = buffer.readInt();
        }
        return new EntityDestroyPacket(entityid);
    }

    @Override
    public ByteBuf encode(EntityDestroyPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(packet.getId().length);
        for (int i = 0; i < packet.getId().length; ++i) {
            buffer.writeInt(packet.getId()[i]);
        }
        return buffer;
    }
}

