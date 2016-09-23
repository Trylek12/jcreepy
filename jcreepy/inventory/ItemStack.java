
package jcreepy.inventory;

import jcreepy.nbt.CompoundMap;
import jcreepy.util.LogicUtil;

public class ItemStack
implements Cloneable {
    private short id;
    private byte amount;
    private short data;
    private CompoundMap nbtData = null;

    public ItemStack(short id, byte amount) {
        this.id = id;
        this.amount = amount;
    }

    public ItemStack(short id, byte amount, short data) {
        this.id = id;
        this.data = data;
        this.amount = amount;
    }

    public short getId() {
        return this.id;
    }

    public ItemStack setId(int id) {
        this.id = (short)id;
        return this;
    }

    public int getAmount() {
        return this.amount;
    }

    public ItemStack setAmount(byte amount) {
        this.amount = amount;
        return this;
    }

    public ItemStack setData(int data) {
        this.data = (short)data;
        return this;
    }

    public short getData() {
        return this.data;
    }

    public boolean isEmpty() {
        return this.amount == 0;
    }

    public CompoundMap getNBTData() {
        if (this.nbtData == null) {
            return null;
        }
        return new CompoundMap(this.nbtData);
    }

    public ItemStack setNBTData(CompoundMap nbtData) {
        this.nbtData = nbtData == null ? null : new CompoundMap(nbtData);
        return this;
    }

    public boolean equalsIgnoreSize(ItemStack other) {
        if (other == null) {
            return false;
        }
        return this.id == other.id && this.data == other.data && LogicUtil.bothNullOrEqual(this.nbtData, other.nbtData);
    }

    public ItemStack clone() {
        ItemStack newStack = new ItemStack(this.id, this.amount, this.data);
        newStack.setNBTData(this.nbtData);
        return newStack;
    }

    public boolean equals(Object other) {
        if (!(other instanceof ItemStack)) {
            return false;
        }
        ItemStack stack = (ItemStack)other;
        return this.equalsIgnoreSize(stack) && this.amount == stack.amount;
    }
}

