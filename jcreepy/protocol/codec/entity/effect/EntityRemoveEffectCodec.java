
package jcreepy.protocol.codec.entity.effect;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.entity.effect.EntityRemoveEffectPacket;

public class EntityRemoveEffectCodec
extends PacketCodec<EntityRemoveEffectPacket> {
    public EntityRemoveEffectCodec() {
        super(EntityRemoveEffectPacket.class, 42);
    }

    @Override
    public EntityRemoveEffectPacket decode(ByteBuf buffer) throws IOException {
        int id = buffer.readInt();
        byte effect = buffer.readByte();
        return new EntityRemoveEffectPacket(id, effect);
    }

    @Override
    public ByteBuf encode(EntityRemoveEffectPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(5);
        buffer.writeInt(packet.getEntityId());
        buffer.writeByte(packet.getEffect());
        return buffer;
    }
}

