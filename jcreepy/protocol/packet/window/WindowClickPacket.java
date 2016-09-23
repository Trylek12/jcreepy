
package jcreepy.protocol.packet.window;

import jcreepy.inventory.ItemStack;
import jcreepy.protocol.packet.window.WindowPacket;

public final class WindowClickPacket
extends WindowPacket {
    private final int slot;
    private final boolean rightClick;
    private final boolean shift;
    private final int transaction;
    private final ItemStack item;

    public WindowClickPacket(int windowInstanceId, int slot, boolean rightClick, int transaction, boolean shift, ItemStack item) {
        super(windowInstanceId);
        this.slot = slot;
        this.rightClick = rightClick;
        this.transaction = transaction;
        this.shift = shift;
        this.item = item;
    }

    public int getSlot() {
        return this.slot;
    }

    public boolean isRightClick() {
        return this.rightClick;
    }

    public boolean isShift() {
        return this.shift;
    }

    public int getTransaction() {
        return this.transaction;
    }

    public ItemStack get() {
        return this.item;
    }
}

