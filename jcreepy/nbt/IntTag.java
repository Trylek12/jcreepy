
package jcreepy.nbt;

import jcreepy.nbt.Tag;
import jcreepy.nbt.TagType;

public final class IntTag
extends Tag<Integer> {
    private final int value;

    public IntTag(String name, int value) {
        super(TagType.TAG_INT, name);
        this.value = value;
    }

    @Override
    public Integer getValue() {
        return this.value;
    }

    @Override
    public IntTag clone() {
        return new IntTag(this.getName(), this.value);
    }
}

