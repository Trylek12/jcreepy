
package jcreepy.protocol.codec.entity.effect;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.entity.effect.EntityEffectPacket;

public class EntityEffectCodec
extends PacketCodec<EntityEffectPacket> {
    public EntityEffectCodec() {
        super(EntityEffectPacket.class, 41);
    }

    @Override
    public EntityEffectPacket decode(ByteBuf buffer) throws IOException {
        int id = buffer.readInt();
        byte effect = buffer.readByte();
        byte amplifier = buffer.readByte();
        short duration = buffer.readShort();
        return new EntityEffectPacket(id, effect, amplifier, duration);
    }

    @Override
    public ByteBuf encode(EntityEffectPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(8);
        buffer.writeInt(packet.getEntityId());
        buffer.writeByte(packet.getEffect());
        buffer.writeByte(packet.getAmplifier());
        buffer.writeShort(packet.getDuration());
        return buffer;
    }
}

