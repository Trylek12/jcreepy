
package io.netty.buffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledHeapByteBuf;

public abstract class AbstractByteBufAllocator
implements ByteBufAllocator {
    private final boolean directByDefault;
    private final ByteBuf emptyBuf;

    protected AbstractByteBufAllocator() {
        this(false);
    }

    protected AbstractByteBufAllocator(boolean directByDefault) {
        this.directByDefault = directByDefault;
        this.emptyBuf = new UnpooledHeapByteBuf((ByteBufAllocator)this, 0, 0);
    }

    @Override
    public ByteBuf buffer() {
        if (this.directByDefault) {
            return this.directBuffer();
        }
        return this.heapBuffer();
    }

    @Override
    public ByteBuf buffer(int initialCapacity) {
        if (this.directByDefault) {
            return this.directBuffer(initialCapacity);
        }
        return this.heapBuffer(initialCapacity);
    }

    @Override
    public ByteBuf buffer(int initialCapacity, int maxCapacity) {
        if (this.directByDefault) {
            return this.directBuffer(initialCapacity, maxCapacity);
        }
        return this.heapBuffer(initialCapacity, maxCapacity);
    }

    @Override
    public ByteBuf heapBuffer() {
        return this.heapBuffer(256, Integer.MAX_VALUE);
    }

    @Override
    public ByteBuf heapBuffer(int initialCapacity) {
        return this.buffer(initialCapacity, Integer.MAX_VALUE);
    }

    @Override
    public ByteBuf heapBuffer(int initialCapacity, int maxCapacity) {
        if (initialCapacity == 0 && maxCapacity == 0) {
            return this.emptyBuf;
        }
        return this.newHeapBuffer(initialCapacity, maxCapacity);
    }

    @Override
    public ByteBuf directBuffer() {
        return this.directBuffer(256, Integer.MAX_VALUE);
    }

    @Override
    public ByteBuf directBuffer(int initialCapacity) {
        return this.directBuffer(initialCapacity, Integer.MAX_VALUE);
    }

    @Override
    public ByteBuf directBuffer(int initialCapacity, int maxCapacity) {
        if (initialCapacity == 0 && maxCapacity == 0) {
            return this.emptyBuf;
        }
        return this.newDirectBuffer(initialCapacity, maxCapacity);
    }

    protected abstract ByteBuf newHeapBuffer(int var1, int var2);

    protected abstract ByteBuf newDirectBuffer(int var1, int var2);
}

