
package jcreepy.nbt.holder;

import jcreepy.nbt.CompoundMap;
import jcreepy.nbt.CompoundTag;
import jcreepy.nbt.Tag;
import jcreepy.nbt.holder.Field;

public class FieldValue<T> {
    private T value;
    private final Field<T> field;
    private final String key;
    private final T defaultValue;

    public FieldValue(String key, Field<T> field) {
        this(key, field, null);
    }

    public FieldValue(String key, Field<T> field, T defaultValue) {
        this.field = field;
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public T load(CompoundTag tag) {
        Object subTag = tag.getValue().get(this.key);
        if (subTag == null) {
            this.value = this.defaultValue;
            return this.value;
        }
        this.value = this.field.getValue(subTag);
        return this.value;
    }

    public void save(CompoundMap tag) {
        T value = this.value;
        if (value == null && (value = this.defaultValue) == null) {
            return;
        }
        Tag t = this.field.getValue(this.key, value);
        tag.put(t);
    }

    public T get() {
        return this.value;
    }

    public void set(T value) {
        this.value = value;
    }

    public static <T> FieldValue<T> from(String name, Field<T> field, T defaultValue) {
        return new FieldValue<T>(name, field, defaultValue);
    }

    public static <T> FieldValue<T> from(String name, Field<T> field) {
        return new FieldValue<T>(name, field);
    }
}

