
package io.netty.buffer;

import io.netty.buffer.ChannelBuf;
import io.netty.buffer.ChannelBufType;
import io.netty.buffer.MessageBuf;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

final class QueueBackedMessageBuf<T>
implements MessageBuf<T>,
ChannelBuf.Unsafe {
    private final Queue<T> queue;
    private boolean freed;

    QueueBackedMessageBuf(Queue<T> queue) {
        if (queue == null) {
            throw new NullPointerException("queue");
        }
        this.queue = queue;
    }

    @Override
    public ChannelBufType type() {
        return ChannelBufType.MESSAGE;
    }

    @Override
    public boolean add(T e) {
        return this.queue.add(e);
    }

    @Override
    public boolean offer(T e) {
        return this.queue.offer(e);
    }

    @Override
    public T remove() {
        return this.queue.remove();
    }

    @Override
    public T poll() {
        return this.queue.poll();
    }

    @Override
    public T element() {
        return this.queue.element();
    }

    @Override
    public T peek() {
        return this.queue.peek();
    }

    @Override
    public int size() {
        return this.queue.size();
    }

    @Override
    public boolean isEmpty() {
        return this.queue.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.queue.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return this.queue.iterator();
    }

    @Override
    public Object[] toArray() {
        return this.queue.toArray();
    }

    @Override
    public <E> E[] toArray(E[] a) {
        return this.queue.toArray(a);
    }

    @Override
    public boolean remove(Object o) {
        return this.queue.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return this.queue.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return this.queue.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return this.queue.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return this.queue.retainAll(c);
    }

    @Override
    public void clear() {
        this.queue.clear();
    }

    @Override
    public int drainTo(Collection<? super T> c) {
        T o;
        int cnt = 0;
        while ((o = this.poll()) != null) {
            c.add(o);
            ++cnt;
        }
        return cnt;
    }

    @Override
    public int drainTo(Collection<? super T> c, int maxElements) {
        T o;
        int cnt;
        for (cnt = 0; cnt < maxElements && (o = this.poll()) != null; ++cnt) {
            c.add(o);
        }
        return cnt;
    }

    @Override
    public ChannelBuf.Unsafe unsafe() {
        return this;
    }

    @Override
    public boolean isFreed() {
        return this.freed;
    }

    @Override
    public void free() {
        this.freed = true;
    }

    public String toString() {
        return this.queue.toString();
    }
}

