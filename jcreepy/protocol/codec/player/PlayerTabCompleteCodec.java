
package jcreepy.protocol.codec.player;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.PlayerTabCompletePacket;
import jcreepy.protocol.util.ByteBufUtils;

public class PlayerTabCompleteCodec
extends PacketCodec<PlayerTabCompletePacket> {
    public PlayerTabCompleteCodec() {
        super(PlayerTabCompletePacket.class, 203);
    }

    @Override
    public PlayerTabCompletePacket decode(ByteBuf buffer) {
        String message = ByteBufUtils.readString(buffer);
        return new PlayerTabCompletePacket(message);
    }

    @Override
    public ByteBuf encode(PlayerTabCompletePacket packet) {
        ByteBuf buffer = Unpooled.buffer();
        ByteBufUtils.writeString(buffer, packet.getText());
        return buffer;
    }
}

