
package jcreepy.nbt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jcreepy.nbt.Tag;
import jcreepy.nbt.TagType;

public final class ListTag<T extends Tag<?>>
extends Tag<List<T>> {
    private final Class<T> type;
    private final List<T> value;

    public ListTag(String name, Class<T> type, List<T> value) {
        super(TagType.TAG_LIST, name);
        this.type = type;
        this.value = Collections.unmodifiableList(value);
    }

    public Class<T> getElementType() {
        return this.type;
    }

    @Override
    public List<T> getValue() {
        return this.value;
    }

    @Override
    public ListTag<T> clone() {
        ArrayList<Object> newList = new ArrayList<Object>();
        for (Tag v : this.value) {
            newList.add(v.clone());
        }
        return new ListTag<T>(this.getName(), this.type, newList);
    }
}

