
package jcreepy.protocol.codec.player.pos;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.pos.PlayerSpawnPositionPacket;

public final class PlayerSpawnPositionCodec
extends PacketCodec<PlayerSpawnPositionPacket> {
    public PlayerSpawnPositionCodec() {
        super(PlayerSpawnPositionPacket.class, 6);
    }

    @Override
    public PlayerSpawnPositionPacket decode(ByteBuf buffer) throws IOException {
        int x = buffer.readInt();
        int y = buffer.readInt();
        int z = buffer.readInt();
        return new PlayerSpawnPositionPacket(x, y, z);
    }

    @Override
    public ByteBuf encode(PlayerSpawnPositionPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(12);
        buffer.writeInt(packet.getX());
        buffer.writeInt(packet.getY());
        buffer.writeInt(packet.getZ());
        return buffer;
    }
}

