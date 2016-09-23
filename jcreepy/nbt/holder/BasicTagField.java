
package jcreepy.nbt.holder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import jcreepy.nbt.ByteTag;
import jcreepy.nbt.Tag;
import jcreepy.nbt.holder.Field;
import jcreepy.nbt.holder.FieldUtils;

public class BasicTagField<T>
implements Field<T> {
    private static final Map<Class<? extends Tag<?>>, Constructor<Tag<?>>> CONSTRUCTOR_CACHE = new HashMap();
    private final Class<? extends Tag<T>> valueType;

    public BasicTagField(Class<? extends Tag<T>> valueType) {
        this.valueType = valueType;
    }

    @Override
    public T getValue(Tag<?> tag) throws IllegalArgumentException {
        Tag<T> value = FieldUtils.checkTagCast(tag, this.valueType);
        return value.getValue();
    }

    @Override
    public Tag<T> getValue(String name, T value) {
        Constructor<Tag<T>> constr = BasicTagField.getConstructor(this.valueType);
        constr.setAccessible(true);
        try {
            return constr.newInstance(name, value);
        }
        catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException ignore) {
        }
        catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private static <T> Constructor<Tag<T>> getConstructor(Class<? extends Tag<T>> tag) {
        Constructor constructor = CONSTRUCTOR_CACHE.get(tag);
        if (constructor == null) {
            Constructor<?>[] constructors = tag.getConstructors();
            if (constructors.length == 1 && constructors[0].getParameterTypes().length == 2 && String.class.isAssignableFrom(constructors[0].getParameterTypes()[0])) {
                constructor = constructors[0];
                CONSTRUCTOR_CACHE.put(tag, constructor);
            } else {
                throw new IllegalArgumentException(tag + " does not have one constructor with the correct type!");
            }
        }
        return constructor;
    }

    static {
        try {
            CONSTRUCTOR_CACHE.put(ByteTag.class, ByteTag.class.getConstructor(String.class, Byte.TYPE));
        }
        catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}

