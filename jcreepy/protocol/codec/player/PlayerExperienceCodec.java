
package jcreepy.protocol.codec.player;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.PlayerExperiencePacket;

public class PlayerExperienceCodec
extends PacketCodec<PlayerExperiencePacket> {
    public PlayerExperienceCodec() {
        super(PlayerExperiencePacket.class, 43);
    }

    @Override
    public PlayerExperiencePacket decode(ByteBuf buffer) throws IOException {
        float barValue = buffer.readFloat();
        short level = buffer.readShort();
        short totalExp = buffer.readShort();
        return new PlayerExperiencePacket(barValue, level, totalExp);
    }

    @Override
    public ByteBuf encode(PlayerExperiencePacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(8);
        buffer.writeFloat(packet.getBarValue());
        buffer.writeShort(packet.getLevel());
        buffer.writeShort(packet.getTotalExp());
        return buffer;
    }
}

