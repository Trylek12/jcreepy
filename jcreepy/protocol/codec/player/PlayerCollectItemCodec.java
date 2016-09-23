
package jcreepy.protocol.codec.player;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.PlayerCollectItemPacket;

public final class PlayerCollectItemCodec
extends PacketCodec<PlayerCollectItemPacket> {
    public PlayerCollectItemCodec() {
        super(PlayerCollectItemPacket.class, 22);
    }

    @Override
    public PlayerCollectItemPacket decode(ByteBuf buffer) throws IOException {
        int id = buffer.readInt();
        int collector = buffer.readInt();
        return new PlayerCollectItemPacket(id, collector);
    }

    @Override
    public ByteBuf encode(PlayerCollectItemPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(8);
        buffer.writeInt(packet.getEntityId());
        buffer.writeInt(packet.getCollector());
        return buffer;
    }
}

