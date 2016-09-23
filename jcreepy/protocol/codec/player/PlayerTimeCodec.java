
package jcreepy.protocol.codec.player;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.PlayerTimePacket;

public final class PlayerTimeCodec
extends PacketCodec<PlayerTimePacket> {
    public PlayerTimeCodec() {
        super(PlayerTimePacket.class, 4);
    }

    @Override
    public PlayerTimePacket decode(ByteBuf buffer) throws IOException {
        return new PlayerTimePacket(buffer.readLong(), buffer.readLong());
    }

    @Override
    public ByteBuf encode(PlayerTimePacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(16);
        buffer.writeLong(packet.getAge());
        buffer.writeLong(packet.getTime());
        return buffer;
    }
}

