
package io.netty.buffer;

import io.netty.buffer.ChannelBuf;
import java.util.Collection;
import java.util.Queue;

public interface MessageBuf<T>
extends ChannelBuf,
Queue<T> {
    public int drainTo(Collection<? super T> var1);

    public int drainTo(Collection<? super T> var1, int var2);
}

