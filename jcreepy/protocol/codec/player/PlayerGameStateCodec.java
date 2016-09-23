
package jcreepy.protocol.codec.player;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.data.GameMode;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.PlayerGameStatePacket;

public final class PlayerGameStateCodec
extends PacketCodec<PlayerGameStatePacket> {
    public PlayerGameStateCodec() {
        super(PlayerGameStatePacket.class, 70);
    }

    @Override
    public PlayerGameStatePacket decode(ByteBuf buffer) throws IOException {
        byte reason = buffer.readByte();
        byte gameMode = buffer.readByte();
        return new PlayerGameStatePacket(reason, GameMode.get(gameMode));
    }

    @Override
    public ByteBuf encode(PlayerGameStatePacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(2);
        buffer.writeByte(packet.getReason());
        buffer.writeByte(packet.getGameMode().getId());
        return buffer;
    }
}

