
package io.netty.buffer;

import io.netty.buffer.ByteBuf;
import java.util.concurrent.TimeUnit;

public interface ByteBufAllocator {
    public ByteBuf buffer();

    public ByteBuf buffer(int var1);

    public ByteBuf buffer(int var1, int var2);

    public ByteBuf heapBuffer();

    public ByteBuf heapBuffer(int var1);

    public ByteBuf heapBuffer(int var1, int var2);

    public ByteBuf directBuffer();

    public ByteBuf directBuffer(int var1);

    public ByteBuf directBuffer(int var1, int var2);

    public ByteBuf ioBuffer();

    public void shutdown();

    public boolean isShutdown();

    public boolean isTerminated();

    public boolean awaitTermination(long var1, TimeUnit var3) throws InterruptedException;
}

