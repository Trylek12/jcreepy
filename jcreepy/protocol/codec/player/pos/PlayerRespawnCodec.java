
package jcreepy.protocol.codec.player.pos;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.pos.PlayerRespawnPacket;
import jcreepy.protocol.util.ByteBufUtils;

public final class PlayerRespawnCodec
extends PacketCodec<PlayerRespawnPacket> {
    public PlayerRespawnCodec() {
        super(PlayerRespawnPacket.class, 9);
    }

    @Override
    public PlayerRespawnPacket decode(ByteBuf buffer) throws IOException {
        int dimension = buffer.readInt();
        byte difficulty = buffer.readByte();
        byte creative = buffer.readByte();
        int height = buffer.readUnsignedShort();
        String worldType = ByteBufUtils.readString(buffer);
        return new PlayerRespawnPacket(dimension, difficulty, creative, height, worldType);
    }

    @Override
    public ByteBuf encode(PlayerRespawnPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(packet.getDimension());
        buffer.writeByte(packet.getDifficulty());
        buffer.writeByte(packet.getGameMode());
        buffer.writeShort(packet.getWorldHeight());
        ByteBufUtils.writeString(buffer, packet.getWorldType());
        return buffer;
    }
}

