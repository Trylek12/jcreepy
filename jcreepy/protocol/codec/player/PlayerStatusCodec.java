
package jcreepy.protocol.codec.player;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.PlayerStatusPacket;

public final class PlayerStatusCodec
extends PacketCodec<PlayerStatusPacket> {
    public PlayerStatusCodec() {
        super(PlayerStatusPacket.class, 205);
    }

    @Override
    public PlayerStatusPacket decode(ByteBuf buffer) throws IOException {
        byte status = buffer.readByte();
        return new PlayerStatusPacket(status);
    }

    @Override
    public ByteBuf encode(PlayerStatusPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(1);
        buffer.writeByte(packet.getStatus());
        return buffer;
    }
}

