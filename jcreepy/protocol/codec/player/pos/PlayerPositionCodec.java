
package jcreepy.protocol.codec.player.pos;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.pos.PlayerPositionPacket;

public final class PlayerPositionCodec
extends PacketCodec<PlayerPositionPacket> {
    public PlayerPositionCodec() {
        super(PlayerPositionPacket.class, 11);
    }

    @Override
    public PlayerPositionPacket decode(ByteBuf buffer) throws IOException {
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double stance = buffer.readDouble();
        double z = buffer.readDouble();
        boolean flying = buffer.readByte() == 1;
        return new PlayerPositionPacket(x, y, z, stance, flying);
    }

    @Override
    public ByteBuf encode(PlayerPositionPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(33);
        buffer.writeDouble(packet.getX());
        buffer.writeDouble(packet.getY());
        buffer.writeDouble(packet.getStance());
        buffer.writeDouble(packet.getZ());
        buffer.writeByte(packet.isOnGround() ? 1 : 0);
        return buffer;
    }
}

