
package jcreepy.protocol.codec.player;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.PlayerChatPacket;
import jcreepy.protocol.util.ByteBufUtils;

public final class PlayerChatCodec
extends PacketCodec<PlayerChatPacket> {
    public PlayerChatCodec() {
        super(PlayerChatPacket.class, 3);
    }

    @Override
    public PlayerChatPacket decode(ByteBuf buffer) {
        String message = ByteBufUtils.readString(buffer);
        return new PlayerChatPacket(message);
    }

    @Override
    public ByteBuf encode(PlayerChatPacket packet) {
        ByteBuf buffer = Unpooled.buffer();
        ByteBufUtils.writeString(buffer, packet.getMessage());
        return buffer;
    }
}

