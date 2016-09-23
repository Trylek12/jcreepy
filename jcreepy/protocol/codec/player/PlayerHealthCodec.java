
package jcreepy.protocol.codec.player;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.PlayerHealthPacket;

public final class PlayerHealthCodec
extends PacketCodec<PlayerHealthPacket> {
    public PlayerHealthCodec() {
        super(PlayerHealthPacket.class, 8);
    }

    @Override
    public PlayerHealthPacket decode(ByteBuf buffer) throws IOException {
        short health = buffer.readShort();
        short food = buffer.readShort();
        float foodSaturation = buffer.readFloat();
        return new PlayerHealthPacket(health, food, foodSaturation);
    }

    @Override
    public ByteBuf encode(PlayerHealthPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(8);
        buffer.writeShort(packet.getHealth());
        buffer.writeShort(packet.getFood());
        buffer.writeFloat(packet.getFoodSaturation());
        return buffer;
    }
}

