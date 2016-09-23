
package jcreepy.protocol.codec.player;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.PlayerGroundPacket;

public final class PlayerGroundCodec
extends PacketCodec<PlayerGroundPacket> {
    public PlayerGroundCodec() {
        super(PlayerGroundPacket.class, 10);
    }

    @Override
    public PlayerGroundPacket decode(ByteBuf buffer) throws IOException {
        return new PlayerGroundPacket(buffer.readByte() == 1);
    }

    @Override
    public ByteBuf encode(PlayerGroundPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(1);
        buffer.writeByte(packet.isOnGround() ? 1 : 0);
        return buffer;
    }
}

