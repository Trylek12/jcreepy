
package jcreepy.protocol.codec.player;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.PlayerStatisticPacket;

public final class PlayerStatisticCodec
extends PacketCodec<PlayerStatisticPacket> {
    public PlayerStatisticCodec() {
        super(PlayerStatisticPacket.class, 200);
    }

    @Override
    public PlayerStatisticPacket decode(ByteBuf buffer) throws IOException {
        int id = buffer.readInt();
        byte amount = buffer.readByte();
        return new PlayerStatisticPacket(id, amount);
    }

    @Override
    public ByteBuf encode(PlayerStatisticPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(5);
        buffer.writeInt(packet.getId());
        buffer.writeByte(packet.getAmount());
        return buffer;
    }
}

