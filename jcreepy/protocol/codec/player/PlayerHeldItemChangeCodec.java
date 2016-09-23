
package jcreepy.protocol.codec.player;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.PlayerHeldItemChangePacket;

public final class PlayerHeldItemChangeCodec
extends PacketCodec<PlayerHeldItemChangePacket> {
    public PlayerHeldItemChangeCodec() {
        super(PlayerHeldItemChangePacket.class, 16);
    }

    @Override
    public PlayerHeldItemChangePacket decode(ByteBuf buffer) throws IOException {
        int slot = buffer.readUnsignedShort();
        return new PlayerHeldItemChangePacket(slot);
    }

    @Override
    public ByteBuf encode(PlayerHeldItemChangePacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(2);
        buffer.writeShort(packet.getSlot());
        return buffer;
    }
}

