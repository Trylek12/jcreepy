
package jcreepy.nbt;

import jcreepy.nbt.Tag;
import jcreepy.nbt.TagType;

public final class DoubleTag
extends Tag<Double> {
    private final double value;

    public DoubleTag(String name, double value) {
        super(TagType.TAG_DOUBLE, name);
        this.value = value;
    }

    @Override
    public Double getValue() {
        return this.value;
    }

    @Override
    public DoubleTag clone() {
        return new DoubleTag(this.getName(), this.value);
    }
}

