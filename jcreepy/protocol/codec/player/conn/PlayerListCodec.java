
package jcreepy.protocol.codec.player.conn;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.conn.PlayerListPacket;
import jcreepy.protocol.util.ByteBufUtils;

public class PlayerListCodec
extends PacketCodec<PlayerListPacket> {
    public PlayerListCodec() {
        super(PlayerListPacket.class, 201);
    }

    @Override
    public PlayerListPacket decode(ByteBuf buffer) throws IOException {
        String name = ByteBufUtils.readString(buffer);
        boolean addOrRemove = buffer.readByte() == 1;
        short ping = buffer.readShort();
        return new PlayerListPacket(name, addOrRemove, ping);
    }

    @Override
    public ByteBuf encode(PlayerListPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        ByteBufUtils.writeString(buffer, packet.getPlayerName());
        buffer.writeByte(packet.playerIsOnline() ? 1 : 0);
        buffer.writeShort(packet.getPing());
        return buffer;
    }
}

