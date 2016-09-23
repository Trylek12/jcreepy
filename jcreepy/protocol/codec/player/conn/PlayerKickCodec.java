
package jcreepy.protocol.codec.player.conn;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.conn.PlayerKickPacket;
import jcreepy.protocol.util.ByteBufUtils;

public final class PlayerKickCodec
extends PacketCodec<PlayerKickPacket> {
    public PlayerKickCodec() {
        super(PlayerKickPacket.class, 255);
    }

    @Override
    public PlayerKickPacket decode(ByteBuf buffer) {
        return new PlayerKickPacket(ByteBufUtils.readString(buffer));
    }

    @Override
    public ByteBuf encode(PlayerKickPacket packet) {
        ByteBuf buffer = Unpooled.buffer();
        ByteBufUtils.writeString(buffer, packet.getReason());
        return buffer;
    }
}

