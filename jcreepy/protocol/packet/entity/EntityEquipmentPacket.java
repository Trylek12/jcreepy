
package jcreepy.protocol.packet.entity;

import jcreepy.inventory.ItemStack;
import jcreepy.protocol.packet.entity.EntityPacket;

public final class EntityEquipmentPacket
extends EntityPacket {
    public static final int HELD_ITEM = 0;
    public static final int BOOTS_SLOT = 1;
    public static final int LEGGINGS_SLOT = 2;
    public static final int CHESTPLATE_SLOT = 3;
    public static final int HELMET_SLOT = 4;
    private final int slot;
    private final ItemStack item;

    public EntityEquipmentPacket(int entityId, int slot, ItemStack item) {
        super(entityId);
        this.slot = slot;
        this.item = item;
    }

    public int getSlot() {
        return this.slot;
    }

    public ItemStack get() {
        return this.item;
    }
}

