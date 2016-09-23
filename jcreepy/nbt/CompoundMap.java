
package jcreepy.nbt;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import jcreepy.nbt.Tag;

public class CompoundMap
implements Map<String, Tag<?>>,
Iterable<Tag<?>> {
    private final Map<String, Tag<?>> map;
    private final boolean sort;
    private final boolean reverse;

    public CompoundMap() {
        this(null, false, false);
    }

    public CompoundMap(List<Tag<?>> initial) {
        this(initial, false, false);
    }

    public CompoundMap(Map<String, Tag<?>> initial) {
        this(initial.values(), false, false);
    }

    @Deprecated
    public CompoundMap(HashMap<String, Tag<?>> initial) {
        this(initial);
    }

    public CompoundMap(CompoundMap initial) {
        this(initial.values(), initial.sort, initial.reverse);
    }

    public CompoundMap(boolean sort, boolean reverse) {
        this(null, sort, reverse);
    }

    public CompoundMap(Iterable<Tag<?>> initial, boolean sort, boolean reverse) {
        this.sort = reverse ? true : sort;
        this.reverse = reverse;
        this.map = !sort ? new LinkedHashMap() : (reverse ? new TreeMap(Collections.reverseOrder()) : new TreeMap());
        if (initial != null) {
            for (Tag t : initial) {
                this.put(t);
            }
        }
    }

    public Tag<?> put(Tag<?> tag) {
        return this.map.put(tag.getName(), tag);
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.map.containsValue(value);
    }

    @Override
    public Set<Map.Entry<String, Tag<?>>> entrySet() {
        return this.map.entrySet();
    }

    @Override
    public Tag<?> get(Object key) {
        return this.map.get(key);
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public Set<String> keySet() {
        return this.map.keySet();
    }

    @Override
    public Tag<?> put(String key, Tag<?> value) {
        return this.map.put(key, value);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Tag<?>> values) {
        this.map.putAll(values);
    }

    @Override
    public Tag remove(Object key) {
        return this.map.remove(key);
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public Collection<Tag<?>> values() {
        return this.map.values();
    }

    @Override
    public Iterator<Tag<?>> iterator() {
        return this.values().iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CompoundMap) {
            CompoundMap other = (CompoundMap)o;
            Iterator iThis = this.iterator();
            Iterator iOther = other.iterator();
            while (iThis.hasNext() && iOther.hasNext()) {
                Tag tOther;
                Tag tThis = iThis.next();
                if (tThis.equals(tOther = iOther.next())) continue;
                return false;
            }
            if (iThis.hasNext() || iOther.hasNext()) {
                return false;
            }
            return true;
        }
        return false;
    }
}

