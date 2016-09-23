
package jcreepy.nbt.holder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import jcreepy.nbt.CompoundMap;
import jcreepy.nbt.CompoundTag;
import jcreepy.nbt.Tag;
import jcreepy.nbt.holder.Field;
import jcreepy.nbt.holder.FieldHolder;
import jcreepy.nbt.holder.FieldUtils;

public class FieldHolderField<T extends FieldHolder>
implements Field<T> {
    private final Class<T> type;
    private final Constructor<T> typeConst;

    public FieldHolderField(Class<T> type) {
        this.type = type;
        try {
            this.typeConst = type.getConstructor(new Class[0]);
            this.typeConst.setAccessible(true);
        }
        catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError("Type must have zero-arg constructor!");
        }
    }

    @Override
    public T getValue(Tag<?> rawTag) throws IllegalArgumentException {
        CompoundTag tag = (CompoundTag)FieldUtils.checkTagCast(rawTag, CompoundTag.class);
        FieldHolder value = null;
        try {
            value = (FieldHolder)this.typeConst.newInstance(new Object[0]);
            value.load(tag);
        }
        catch (InstantiationException e) {
        }
        catch (IllegalAccessException e) {
        }
        catch (InvocationTargetException e) {
            // empty catch block
        }
        return (T)value;
    }

    @Override
    public Tag<?> getValue(String name, T value) {
        return new CompoundTag(name, value.save());
    }
}

