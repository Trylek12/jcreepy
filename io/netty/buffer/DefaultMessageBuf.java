
package io.netty.buffer;

import io.netty.buffer.ChannelBuf;
import io.netty.buffer.ChannelBufType;
import io.netty.buffer.MessageBuf;
import java.util.ArrayDeque;
import java.util.Collection;

final class DefaultMessageBuf<T>
extends ArrayDeque<T>
implements MessageBuf<T>,
ChannelBuf.Unsafe {
    private static final long serialVersionUID = 1229808623624907552L;
    private boolean freed;

    DefaultMessageBuf() {
    }

    DefaultMessageBuf(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    public ChannelBufType type() {
        return ChannelBufType.MESSAGE;
    }

    @Override
    public int drainTo(Collection<? super T> c) {
        Object o;
        int cnt = 0;
        while ((o = this.poll()) != null) {
            c.add(o);
            ++cnt;
        }
        return cnt;
    }

    @Override
    public int drainTo(Collection<? super T> c, int maxElements) {
        Object o;
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
}

