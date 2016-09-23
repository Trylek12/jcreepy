
package jcreepy.nbt;

import jcreepy.nbt.CompoundMap;
import jcreepy.nbt.Tag;
import jcreepy.nbt.TagType;

public final class CompoundTag
extends Tag<CompoundMap> {
    private final CompoundMap value;

    public CompoundTag(String name, CompoundMap value) {
        super(TagType.TAG_COMPOUND, name);
        this.value = value;
    }

    @Override
    public CompoundMap getValue() {
        return this.value;
    }

    @Override
    public CompoundTag clone() {
        CompoundMap map = new CompoundMap(this.value);
        return new CompoundTag(this.getName(), map);
    }
}

