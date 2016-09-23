
package jcreepy.nbt;

import java.util.Arrays;
import jcreepy.nbt.Tag;
import jcreepy.nbt.TagType;

public final class IntArrayTag
extends Tag<int[]> {
    private final int[] value;

    public IntArrayTag(String name, int[] value) {
        super(TagType.TAG_INT_ARRAY, name);
        this.value = value;
    }

    @Override
    public int[] getValue() {
        return this.value;
    }

    @Override
    public IntArrayTag clone() {
        int[] clonedArray = this.cloneArray(this.value);
        return new IntArrayTag(this.getName(), clonedArray);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof IntArrayTag)) {
            return false;
        }
        IntArrayTag tag = (IntArrayTag)other;
        return Arrays.equals(this.value, tag.value) && this.getName().equals(tag.getName());
    }

    private int[] cloneArray(int[] intArray) {
        if (intArray == null) {
            return null;
        }
        int length = intArray.length;
        byte[] newArray = new byte[length];
        System.arraycopy(intArray, 0, newArray, 0, length);
        return intArray;
    }
}

