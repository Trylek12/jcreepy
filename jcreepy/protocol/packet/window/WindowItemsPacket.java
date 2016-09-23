
package jcreepy.protocol.packet.window;

import jcreepy.inventory.ItemStack;
import jcreepy.protocol.packet.window.WindowPacket;

public final class WindowItemsPacket
extends WindowPacket {
    private final ItemStack[] items;

    public WindowItemsPacket(int windowInstanceId, ItemStack[] items) {
        super(windowInstanceId);
        this.items = items;
    }

    public ItemStack[] getItems() {
        return this.items;
    }
}

