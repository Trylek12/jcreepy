
package jcreepy.nbt;

import jcreepy.nbt.Tag;
import jcreepy.nbt.TagType;

public final class ShortTag
extends Tag<Short> {
    private final short value;

    public ShortTag(String name, short value) {
        super(TagType.TAG_SHORT, name);
        this.value = value;
    }

    @Override
    public Short getValue() {
        return this.value;
    }

    @Override
    public ShortTag clone() {
        return new ShortTag(this.getName(), this.value);
    }
}

