
package jcreepy.protocol.codec.player;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.PlayerLocaleViewDistancePacket;
import jcreepy.protocol.util.ByteBufUtils;

public class PlayerLocaleViewDistanceCodec
extends PacketCodec<PlayerLocaleViewDistancePacket> {
    public PlayerLocaleViewDistanceCodec() {
        super(PlayerLocaleViewDistancePacket.class, 204);
    }

    @Override
    public PlayerLocaleViewDistancePacket decode(ByteBuf buffer) throws IOException {
        String locale = ByteBufUtils.readString(buffer);
        byte viewDistance = buffer.readByte();
        byte chatFlags = buffer.readByte();
        byte difficulty = buffer.readByte();
        boolean showCape = buffer.readByte() != 0;
        return new PlayerLocaleViewDistancePacket(locale, viewDistance, chatFlags, difficulty, showCape);
    }

    @Override
    public ByteBuf encode(PlayerLocaleViewDistancePacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        ByteBufUtils.writeString(buffer, packet.getLocale());
        buffer.writeByte(packet.getViewDistance());
        buffer.writeByte(packet.getChatFlags());
        buffer.writeByte(packet.getDifficulty());
        buffer.writeByte(packet.showsCape() ? 1 : 0);
        return buffer;
    }
}

