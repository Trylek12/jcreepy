
package jline.console.history;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import jline.console.history.History;
import jline.internal.Preconditions;


public class MemoryHistory
implements History {
    public static final int DEFAULT_MAX_SIZE = 500;
    private final LinkedList<CharSequence> items = new LinkedList();
    private int maxSize = 500;
    private boolean ignoreDuplicates = true;
    private boolean autoTrim = false;
    private int offset = 0;
    private int index = 0;

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
        this.maybeResize();
    }

    public int getMaxSize() {
        return this.maxSize;
    }

    public boolean isIgnoreDuplicates() {
        return this.ignoreDuplicates;
    }

    public void setIgnoreDuplicates(boolean flag) {
        this.ignoreDuplicates = flag;
    }

    public boolean isAutoTrim() {
        return this.autoTrim;
    }

    public void setAutoTrim(boolean flag) {
        this.autoTrim = flag;
    }

    @Override
    public int size() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    @Override
    public int index() {
        return this.offset + this.index;
    }

    @Override
    public void clear() {
        this.items.clear();
        this.offset = 0;
        this.index = 0;
    }

    @Override
    public CharSequence get(int index) {
        return this.items.get(index - this.offset);
    }

    @Override
    public void set(int index, CharSequence item) {
        this.items.set(index - this.offset, item);
    }

    @Override
    public void add(CharSequence item) {
        Preconditions.checkNotNull(item);
        if (this.isAutoTrim()) {
            item = String.valueOf(item).trim();
        }
        if (this.isIgnoreDuplicates() && !this.items.isEmpty() && item.equals(this.items.getLast())) {
            return;
        }
        this.internalAdd(item);
    }

    @Override
    public CharSequence remove(int i) {
        return this.items.remove(i);
    }

    @Override
    public CharSequence removeFirst() {
        return this.items.removeFirst();
    }

    @Override
    public CharSequence removeLast() {
        return this.items.removeLast();
    }

    protected void internalAdd(CharSequence item) {
        this.items.add(item);
        this.maybeResize();
    }

    @Override
    public void replace(CharSequence item) {
        this.items.removeLast();
        this.add(item);
    }

    private void maybeResize() {
        while (this.size() > this.getMaxSize()) {
            this.items.removeFirst();
            ++this.offset;
        }
        this.index = this.size();
    }

    @Override
    public ListIterator<History.Entry> entries(int index) {
        return new EntriesIterator(index - this.offset);
    }

    @Override
    public ListIterator<History.Entry> entries() {
        return this.entries(this.offset);
    }

    @Override
    public Iterator<History.Entry> iterator() {
        return this.entries();
    }

    @Override
    public boolean moveToLast() {
        int lastEntry = this.size() - 1;
        if (lastEntry >= 0 && lastEntry != this.index) {
            this.index = this.size() - 1;
            return true;
        }
        return false;
    }

    @Override
    public boolean moveTo(int index) {
        if ((index -= this.offset) >= 0 && index < this.size()) {
            this.index = index;
            return true;
        }
        return false;
    }

    @Override
    public boolean moveToFirst() {
        if (this.size() > 0 && this.index != 0) {
            this.index = 0;
            return true;
        }
        return false;
    }

    @Override
    public void moveToEnd() {
        this.index = this.size();
    }

    @Override
    public CharSequence current() {
        if (this.index >= this.size()) {
            return "";
        }
        return this.items.get(this.index);
    }

    @Override
    public boolean previous() {
        if (this.index <= 0) {
            return false;
        }
        --this.index;
        return true;
    }

    @Override
    public boolean next() {
        if (this.index >= this.size()) {
            return false;
        }
        ++this.index;
        return true;
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private class EntriesIterator
    implements ListIterator<History.Entry> {
        private final ListIterator<CharSequence> source;

        private EntriesIterator(int index) {
            this.source = MemoryHistory.this.items.listIterator(index);
        }

        @Override
        public History.Entry next() {
            if (!this.source.hasNext()) {
                throw new NoSuchElementException();
            }
            return new EntryImpl(MemoryHistory.this.offset + this.source.nextIndex(), this.source.next());
        }

        @Override
        public History.Entry previous() {
            if (!this.source.hasPrevious()) {
                throw new NoSuchElementException();
            }
            return new EntryImpl(MemoryHistory.this.offset + this.source.previousIndex(), this.source.previous());
        }

        @Override
        public int nextIndex() {
            return MemoryHistory.this.offset + this.source.nextIndex();
        }

        @Override
        public int previousIndex() {
            return MemoryHistory.this.offset + this.source.previousIndex();
        }

        @Override
        public boolean hasNext() {
            return this.source.hasNext();
        }

        @Override
        public boolean hasPrevious() {
            return this.source.hasPrevious();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(History.Entry entry) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(History.Entry entry) {
            throw new UnsupportedOperationException();
        }
    }

    private static class EntryImpl
    implements History.Entry {
        private final int index;
        private final CharSequence value;

        public EntryImpl(int index, CharSequence value) {
            this.index = index;
            this.value = value;
        }

        public int index() {
            return this.index;
        }

        public CharSequence value() {
            return this.value;
        }

        public String toString() {
            return String.format("%d: %s", this.index, this.value);
        }
    }

}

