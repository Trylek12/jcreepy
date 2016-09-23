
package jcreepy.nbt.holder;

import jcreepy.nbt.Tag;

public interface Field<T> {
    public T getValue(Tag<?> var1) throws IllegalArgumentException;

    public Tag<?> getValue(String var1, T var2);
}

