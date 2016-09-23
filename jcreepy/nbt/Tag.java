
package jcreepy.nbt;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import jcreepy.nbt.TagType;

public abstract class Tag<T>
implements Comparable<Tag<?>> {
    private final String name;
    private final TagType type;

    public Tag(TagType type) {
        this(type, "");
    }

    public Tag(TagType type, String name) {
        this.name = name;
        this.type = type;
    }

    public final String getName() {
        return this.name;
    }

    public TagType getType() {
        return this.type;
    }

    public abstract T getValue();

    public static Map<String, Tag<?>> cloneMap(Map<String, Tag<?>> map) {
        if (map == null) {
            return null;
        }
        HashMap<String, Tag<?>> newMap = new HashMap<String, Tag<?>>();
        for (Map.Entry<String, Tag<?>> entry : map.entrySet()) {
            newMap.put(entry.getKey(), (Object)entry.getValue().clone());
        }
        return newMap;
    }

    public boolean equals(Object other) {
        if (!(other instanceof Tag)) {
            return false;
        }
        Tag tag = (Tag)other;
        return this.getValue().equals(tag.getValue()) && this.getName().equals(tag.getName());
    }

    @Override
    public int compareTo(Tag other) {
        if (this.equals(other)) {
            return 0;
        }
        if (other.getName().equals(this.getName())) {
            throw new IllegalStateException("Cannot compare two Tags with the same name but different values for sorting");
        }
        return this.getName().compareTo(other.getName());
    }

    public abstract Tag<T> clone();
}

