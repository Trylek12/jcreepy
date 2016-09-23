
package jcreepy.nbt;

import jcreepy.nbt.Tag;
import jcreepy.nbt.TagType;

public final class ByteTag
extends Tag<Byte> {
    private final byte value;

    public ByteTag(String name, boolean value) {
        this(name, value ? 1 : 0);
    }

    public ByteTag(String name, byte value) {
        super(TagType.TAG_BYTE, name);
        this.value = value;
    }

    @Override
    public Byte getValue() {
        return Byte.valueOf(this.value);
    }

    public boolean getBooleanValue() {
        return this.value != 0;
    }

    @Override
    public ByteTag clone() {
        return new ByteTag(this.getName(), this.value);
    }
}

