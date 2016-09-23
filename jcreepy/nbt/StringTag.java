
package jcreepy.nbt;

import jcreepy.nbt.Tag;
import jcreepy.nbt.TagType;

public final class StringTag
extends Tag<String> {
    private final String value;

    public StringTag(String name, String value) {
        super(TagType.TAG_STRING, name);
        this.value = value;
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public StringTag clone() {
        return new StringTag(this.getName(), this.value);
    }
}

