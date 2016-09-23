
package jcreepy.nbt.holder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import jcreepy.nbt.ListTag;
import jcreepy.nbt.Tag;
import jcreepy.nbt.holder.Field;
import jcreepy.nbt.holder.FieldUtils;

public class ListField<T>
implements Field<List<T>> {
    private final Field<T> backingField;

    public ListField(Field<T> field) {
        this.backingField = field;
    }

    @Override
    public List<T> getValue(Tag<?> tag) throws IllegalArgumentException {
        ListTag listTag = (ListTag)FieldUtils.checkTagCast(tag, ListTag.class);
        ArrayList<T> result = new ArrayList<T>();
        Iterator i$ = listTag.getValue().iterator();
        while (i$.hasNext()) {
            Tag element = (Tag)i$.next();
            result.add(this.backingField.getValue(element));
        }
        return result;
    }

    @Override
    public Tag<?> getValue(String name, List<T> value) {
        ArrayList tags = new ArrayList();
        Class tagClazz = Tag.class;
        for (T element : value) {
            Tag tag = this.backingField.getValue("", element);
            tagClazz = tag.getClass();
            tags.add(tag);
        }
        return new ListTag(name, tagClazz, tags);
    }
}

