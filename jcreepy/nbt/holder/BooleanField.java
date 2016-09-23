
package jcreepy.nbt.holder;

import jcreepy.nbt.ByteTag;
import jcreepy.nbt.Tag;
import jcreepy.nbt.holder.Field;
import jcreepy.nbt.holder.FieldUtils;

public class BooleanField
implements Field<Boolean> {
    public static final BooleanField INSTANCE = new BooleanField();

    @Override
    public Boolean getValue(Tag<?> tag) throws IllegalArgumentException {
        return ((ByteTag)FieldUtils.checkTagCast(tag, ByteTag.class)).getBooleanValue();
    }

    @Override
    public Tag<?> getValue(String name, Boolean value) {
        return new ByteTag(name, value);
    }
}

