
package jcreepy.protocol.codec.world;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.world.ExplosionPacket;

public final class ExplosionCodec
extends PacketCodec<ExplosionPacket> {
    public ExplosionCodec() {
        super(ExplosionPacket.class, 60);
    }

    @Override
    public ExplosionPacket decode(ByteBuf buffer) throws IOException {
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();
        float radius = buffer.readFloat();
        int records = buffer.readInt();
        byte[] coordinates = new byte[records * 3];
        buffer.readBytes(coordinates);
        buffer.readFloat();
        buffer.readFloat();
        buffer.readFloat();
        return new ExplosionPacket(x, y, z, radius, coordinates);
    }

    @Override
    public ByteBuf encode(ExplosionPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeDouble(packet.getX());
        buffer.writeDouble(packet.getY());
        buffer.writeDouble(packet.getZ());
        buffer.writeFloat(packet.getRadius());
        buffer.writeInt(packet.getRecords());
        buffer.writeBytes(packet.getCoordinates());
        buffer.writeFloat(0.0f);
        buffer.writeFloat(0.0f);
        buffer.writeFloat(0.0f);
        return buffer;
    }
}

