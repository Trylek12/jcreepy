
package jcreepy.nbt;

import jcreepy.nbt.Tag;
import jcreepy.nbt.TagType;

public final class EndTag
extends Tag<Object> {
    public EndTag() {
        super(TagType.TAG_END);
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public EndTag clone() {
        return new EndTag();
    }
}

