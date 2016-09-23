
package jcreepy.protocol.codec.entity;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.entity.EntityItemDataPacket;

public final class EntityItemDataCodec
extends PacketCodec<EntityItemDataPacket> {
    public EntityItemDataCodec() {
        super(EntityItemDataPacket.class, 131);
    }

    @Override
    public EntityItemDataPacket decode(ByteBuf buffer) throws IOException {
        short type = buffer.readShort();
        short id = buffer.readShort();
        int size = buffer.readUnsignedShort();
        byte[] data = new byte[size];
        buffer.readBytes(data);
        return new EntityItemDataPacket(type, id, data);
    }

    @Override
    public ByteBuf encode(EntityItemDataPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeShort(packet.getType());
        buffer.writeShort(packet.getId());
        buffer.writeShort(packet.getData().length);
        buffer.writeBytes(packet.getData());
        return buffer;
    }
}

