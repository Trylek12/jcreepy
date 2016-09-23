
package jcreepy.protocol.packet.entity;

import jcreepy.inventory.ItemStack;
import jcreepy.network.Packet;

public final class EntityItemDataPacket
extends Packet {
    private final short type;
    private final short id;
    private final byte[] data;

    public EntityItemDataPacket(ItemStack item, byte[] data) {
        this(item.getId(), item.getData(), data);
    }

    public EntityItemDataPacket(short type, short id, byte[] data) {
        this.type = type;
        this.id = id;
        this.data = data;
    }

    public short getType() {
        return this.type;
    }

    public short getId() {
        return this.id;
    }

    public byte[] getData() {
        return this.data;
    }
}

