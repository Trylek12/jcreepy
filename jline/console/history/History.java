
package jline.console.history;

import java.util.Iterator;
import java.util.ListIterator;

public interface History
extends Iterable<Entry> {
    public int size();

    public boolean isEmpty();

    public int index();

    public void clear();

    public CharSequence get(int var1);

    public void add(CharSequence var1);

    public void set(int var1, CharSequence var2);

    public CharSequence remove(int var1);

    public CharSequence removeFirst();

    public CharSequence removeLast();

    public void replace(CharSequence var1);

    public ListIterator<Entry> entries(int var1);

    public ListIterator<Entry> entries();

    @Override
    public Iterator<Entry> iterator();

    public CharSequence current();

    public boolean previous();

    public boolean next();

    public boolean moveToFirst();

    public boolean moveToLast();

    public boolean moveTo(int var1);

    public void moveToEnd();

    public static interface Entry {
        public int index();

        public CharSequence value();
    }

}

