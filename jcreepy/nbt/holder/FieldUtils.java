
package jcreepy.nbt.holder;

import jcreepy.nbt.Tag;

public class FieldUtils {
    private FieldUtils() {
    }

    public static <T extends Tag<?>> T checkTagCast(Tag<?> tag, Class<T> type) throws IllegalArgumentException {
        if (tag == null) {
            throw new IllegalArgumentException("Expected tag of type " + type.getName() + ", was null");
        }
        if (!type.isInstance(tag)) {
            throw new IllegalArgumentException("Expected tag to be a " + type.getName() + ", was a " + tag.getClass().getName());
        }
        return (T)((Tag)type.cast(tag));
    }
}

