
package jcreepy.protocol.packet.window;

import jcreepy.inventory.ItemStack;
import jcreepy.protocol.packet.window.WindowPacket;

public final class WindowSlotPacket
extends WindowPacket {
    private final int slot;
    private final ItemStack item;

    public WindowSlotPacket(int windowInstanceId, int slot, ItemStack item) {
        super(windowInstanceId);
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

