
package jcreepy.protocol.codec.player.pos;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.pos.PlayerPositionYawPacket;

public final class PlayerPositionYawCodec
extends PacketCodec<PlayerPositionYawPacket> {
    public PlayerPositionYawCodec() {
        super(PlayerPositionYawPacket.class, 13);
    }

    @Override
    public PlayerPositionYawPacket decode(ByteBuf buffer) throws IOException {
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double stance = buffer.readDouble();
        double z = buffer.readDouble();
        float yaw = buffer.readFloat();
        float pitch = buffer.readFloat();
        boolean onGround = buffer.readByte() == 1;
        return new PlayerPositionYawPacket(x, y, z, stance, yaw, pitch, onGround);
    }

    @Override
    public ByteBuf encode(PlayerPositionYawPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(41);
        buffer.writeDouble(packet.getX());
        buffer.writeDouble(packet.getY());
        buffer.writeDouble(packet.getStance());
        buffer.writeDouble(packet.getZ());
        buffer.writeFloat(packet.getYaw());
        buffer.writeFloat(packet.getPitch());
        buffer.writeByte(packet.isOnGround() ? 1 : 0);
        return buffer;
    }
}

