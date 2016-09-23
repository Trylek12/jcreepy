
package jcreepy.protocol.codec.world;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.world.EffectPacket;

public final class EffectCodec
extends PacketCodec<EffectPacket> {
    public EffectCodec() {
        super(EffectPacket.class, 61);
    }

    @Override
    public EffectPacket decode(ByteBuf buffer) throws IOException {
        int id = buffer.readInt();
        int x = buffer.readInt();
        short y = buffer.readUnsignedByte();
        int z = buffer.readInt();
        int data = buffer.readInt();
        boolean volumeDecrease = buffer.readByte() != 0;
        return new EffectPacket(id, x, y, z, data, volumeDecrease);
    }

    @Override
    public ByteBuf encode(EffectPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(18);
        buffer.writeInt(packet.getId());
        buffer.writeInt(packet.getX());
        buffer.writeByte(packet.getY());
        buffer.writeInt(packet.getZ());
        buffer.writeInt(packet.getData());
        buffer.writeByte(packet.hasVolumeDecrease() ? 1 : 0);
        return buffer;
    }
}

