
package io.netty.buffer;

import io.netty.buffer.AbstractByteBufAllocator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledDirectByteBuf;
import io.netty.buffer.UnpooledHeapByteBuf;
import io.netty.util.internal.DetectionUtil;
import java.util.concurrent.TimeUnit;

public final class UnpooledByteBufAllocator
extends AbstractByteBufAllocator {
    public static final UnpooledByteBufAllocator HEAP_BY_DEFAULT = new UnpooledByteBufAllocator(false);
    public static final UnpooledByteBufAllocator DIRECT_BY_DEFAULT = new UnpooledByteBufAllocator(true);

    private UnpooledByteBufAllocator(boolean directByDefault) {
        super(directByDefault);
    }

    @Override
    protected ByteBuf newHeapBuffer(int initialCapacity, int maxCapacity) {
        return new UnpooledHeapByteBuf((ByteBufAllocator)this, initialCapacity, maxCapacity);
    }

    @Override
    protected ByteBuf newDirectBuffer(int initialCapacity, int maxCapacity) {
        return new UnpooledDirectByteBuf((ByteBufAllocator)this, initialCapacity, maxCapacity);
    }

    @Override
    public ByteBuf ioBuffer() {
        if (DetectionUtil.canFreeDirectBuffer()) {
            return this.directBuffer();
        }
        return this.heapBuffer();
    }

    @Override
    public void shutdown() {
        throw new IllegalStateException(this.getClass().getName() + " cannot be shut down.");
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        Thread.sleep(unit.toMillis(timeout));
        return false;
    }
}

