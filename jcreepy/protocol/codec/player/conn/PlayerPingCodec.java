
package jcreepy.protocol.codec.player.conn;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.conn.PlayerPingPacket;

public final class PlayerPingCodec
extends PacketCodec<PlayerPingPacket> {
    public PlayerPingCodec() {
        super(PlayerPingPacket.class, 0);
    }

    @Override
    public PlayerPingPacket decode(ByteBuf buffer) {
        int id = buffer.readInt();
        return new PlayerPingPacket(id);
    }

    @Override
    public ByteBuf encode(PlayerPingPacket packet) {
        ByteBuf buffer = Unpooled.buffer(4);
        buffer.writeInt(packet.getPingId());
        return buffer;
    }
}

