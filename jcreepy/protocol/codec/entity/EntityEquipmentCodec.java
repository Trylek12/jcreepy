
package jcreepy.protocol.codec.entity;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.inventory.ItemStack;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.entity.EntityEquipmentPacket;
import jcreepy.protocol.util.ByteBufUtils;

public final class EntityEquipmentCodec
extends PacketCodec<EntityEquipmentPacket> {
    public EntityEquipmentCodec() {
        super(EntityEquipmentPacket.class, 5);
    }

    @Override
    public EntityEquipmentPacket decode(ByteBuf buffer) throws IOException {
        int entityId = buffer.readInt();
        int slot = buffer.readUnsignedShort();
        ItemStack item = ByteBufUtils.readItemStack(buffer);
        return new EntityEquipmentPacket(entityId, slot, item);
    }

    @Override
    public ByteBuf encode(EntityEquipmentPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(packet.getEntityId());
        buffer.writeShort(packet.getSlot());
        ByteBufUtils.writeItemStack(buffer, packet.get());
        return buffer;
    }
}

