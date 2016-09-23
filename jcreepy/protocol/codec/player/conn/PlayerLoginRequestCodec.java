
package jcreepy.protocol.codec.player.conn;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.conn.PlayerLoginRequestPacket;
import jcreepy.protocol.util.ByteBufUtils;

public final class PlayerLoginRequestCodec
extends PacketCodec<PlayerLoginRequestPacket> {
    public PlayerLoginRequestCodec() {
        super(PlayerLoginRequestPacket.class, 1);
    }

    @Override
    public PlayerLoginRequestPacket decode(ByteBuf buffer) {
        int id = buffer.readInt();
        String worldType = ByteBufUtils.readString(buffer);
        byte mode = buffer.readByte();
        byte dimension = buffer.readByte();
        byte difficulty = buffer.readByte();
        buffer.readUnsignedByte();
        short maxPlayers = buffer.readUnsignedByte();
        return new PlayerLoginRequestPacket(id, worldType, mode, dimension, difficulty, maxPlayers);
    }

    @Override
    public ByteBuf encode(PlayerLoginRequestPacket packet) {
        PlayerLoginRequestPacket server = packet;
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(server.getId());
        ByteBufUtils.writeString(buffer, server.getWorldType());
        buffer.writeByte(server.getGameMode());
        buffer.writeByte(server.getDimension());
        buffer.writeByte(server.getDifficulty());
        buffer.writeByte(0);
        buffer.writeByte(server.getMaxPlayers());
        return buffer;
    }
}

