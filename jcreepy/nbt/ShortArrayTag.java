
package jcreepy.nbt;

import java.util.Arrays;
import jcreepy.nbt.Tag;
import jcreepy.nbt.TagType;

public final class ShortArrayTag
extends Tag<short[]> {
    private final short[] value;

    public ShortArrayTag(String name, short[] value) {
        super(TagType.TAG_SHORT_ARRAY, name);
        this.value = value;
    }

    @Override
    public short[] getValue() {
        return this.value;
    }

    @Override
    public ShortArrayTag clone() {
        short[] clonedArray = this.cloneArray(this.value);
        return new ShortArrayTag(this.getName(), clonedArray);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ShortArrayTag)) {
            return false;
        }
        ShortArrayTag tag = (ShortArrayTag)other;
        return Arrays.equals(this.value, tag.value) && this.getName().equals(tag.getName());
    }

    private short[] cloneArray(short[] shortArray) {
        if (shortArray == null) {
            return null;
        }
        int length = shortArray.length;
        short[] newArray = new short[length];
        System.arraycopy(shortArray, 0, newArray, 0, length);
        return shortArray;
    }
}

