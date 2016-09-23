
package jcreepy.protocol.codec.player.pos;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.pos.PlayerYawPacket;

public final class PlayerYawCodec
extends PacketCodec<PlayerYawPacket> {
    public PlayerYawCodec() {
        super(PlayerYawPacket.class, 12);
    }

    @Override
    public PlayerYawPacket decode(ByteBuf buffer) throws IOException {
        float yaw = - buffer.readFloat();
        float pitch = buffer.readFloat();
        boolean onGround = buffer.readByte() == 1;
        return new PlayerYawPacket(yaw, pitch, onGround);
    }

    @Override
    public ByteBuf encode(PlayerYawPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(9);
        buffer.writeFloat(- packet.getYaw());
        buffer.writeFloat(packet.getPitch());
        buffer.writeByte(packet.isOnGround() ? 1 : 0);
        return buffer;
    }
}

