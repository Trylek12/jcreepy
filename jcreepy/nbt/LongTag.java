
package jcreepy.nbt;

import jcreepy.nbt.Tag;
import jcreepy.nbt.TagType;

public final class LongTag
extends Tag<Long> {
    private final long value;

    public LongTag(String name, long value) {
        super(TagType.TAG_LONG, name);
        this.value = value;
    }

    @Override
    public Long getValue() {
        return this.value;
    }

    @Override
    public LongTag clone() {
        return new LongTag(this.getName(), this.value);
    }
}

