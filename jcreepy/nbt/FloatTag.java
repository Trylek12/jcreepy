
package jcreepy.nbt;

import jcreepy.nbt.Tag;
import jcreepy.nbt.TagType;

public final class FloatTag
extends Tag<Float> {
    private final float value;

    public FloatTag(String name, float value) {
        super(TagType.TAG_FLOAT, name);
        this.value = value;
    }

    @Override
    public Float getValue() {
        return Float.valueOf(this.value);
    }

    @Override
    public FloatTag clone() {
        return new FloatTag(this.getName(), this.value);
    }
}

