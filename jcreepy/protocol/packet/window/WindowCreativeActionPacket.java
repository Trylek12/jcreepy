
package jcreepy.protocol.packet.window;

import jcreepy.inventory.ItemStack;
import jcreepy.network.Packet;

public final class WindowCreativeActionPacket
extends Packet {
    private final short slot;
    private final ItemStack item;

    public WindowCreativeActionPacket(short slot, ItemStack item) {
        this.slot = slot;
        this.item = item;
    }

    public short getSlot() {
        return this.slot;
    }

    public ItemStack get() {
        return this.item;
    }
}

