
package jcreepy.protocol.codec.entity;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.nbt.CompoundMap;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.entity.EntityTileDataPacket;
import jcreepy.protocol.util.ByteBufUtils;

public class EntityTileDataCodec
extends PacketCodec<EntityTileDataPacket> {
    public EntityTileDataCodec() {
        super(EntityTileDataPacket.class, 132);
    }

    @Override
    public EntityTileDataPacket decode(ByteBuf buffer) throws IOException {
        int x = buffer.readInt();
        short y = buffer.readShort();
        int z = buffer.readInt();
        byte action = buffer.readByte();
        CompoundMap data = ByteBufUtils.readCompound(buffer);
        return new EntityTileDataPacket(x, (int)y, z, (int)action, data);
    }

    @Override
    public ByteBuf encode(EntityTileDataPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(packet.getX());
        buffer.writeShort(packet.getY());
        buffer.writeInt(packet.getZ());
        buffer.writeByte(packet.getAction());
        ByteBufUtils.writeCompound(buffer, packet.getData());
        return buffer;
    }
}

