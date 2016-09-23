
package io.netty.channel;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelOption;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public class DefaultChannelConfig
implements ChannelConfig {
    private static final ByteBufAllocator DEFAULT_ALLOCATOR = UnpooledByteBufAllocator.HEAP_BY_DEFAULT;
    private static final int DEFAULT_CONNECT_TIMEOUT = 30000;
    private volatile ByteBufAllocator allocator = DEFAULT_ALLOCATOR;
    private volatile int connectTimeoutMillis = 30000;
    private volatile int writeSpinCount = 16;

    @Override
    public Map<ChannelOption<?>, Object> getOptions() {
        return this.getOptions(null, ChannelOption.CONNECT_TIMEOUT_MILLIS, ChannelOption.WRITE_SPIN_COUNT, ChannelOption.ALLOCATOR);
    }

    protected /* varargs */ Map<ChannelOption<?>, Object> getOptions(Map<ChannelOption<?>, Object> result, ChannelOption<?> ... options) {
        if (result == null) {
            result = new IdentityHashMap();
        }
        for (ChannelOption o : options) {
            result.put(o, this.getOption(o));
        }
        return result;
    }

    @Override
    public boolean setOptions(Map<ChannelOption<?>, ?> options) {
        if (options == null) {
            throw new NullPointerException("options");
        }
        boolean setAllOptions = true;
        for (Map.Entry e : options.entrySet()) {
            if (this.setOption(e.getKey(), e.getValue())) continue;
            setAllOptions = false;
        }
        return setAllOptions;
    }

    @Override
    public <T> T getOption(ChannelOption<T> option) {
        if (option == null) {
            throw new NullPointerException("option");
        }
        if (option == ChannelOption.CONNECT_TIMEOUT_MILLIS) {
            return this.getConnectTimeoutMillis();
        }
        if (option == ChannelOption.WRITE_SPIN_COUNT) {
            return this.getWriteSpinCount();
        }
        if (option == ChannelOption.ALLOCATOR) {
            return (T)this.getAllocator();
        }
        return null;
    }

    @Override
    public <T> boolean setOption(ChannelOption<T> option, T value) {
        this.validate(option, value);
        if (option == ChannelOption.CONNECT_TIMEOUT_MILLIS) {
            this.setConnectTimeoutMillis((Integer)value);
        } else if (option == ChannelOption.WRITE_SPIN_COUNT) {
            this.setWriteSpinCount((Integer)value);
        } else if (option == ChannelOption.ALLOCATOR) {
            this.setAllocator((ByteBufAllocator)value);
        } else {
            return false;
        }
        return true;
    }

    protected <T> void validate(ChannelOption<T> option, T value) {
        if (option == null) {
            throw new NullPointerException("option");
        }
        option.validate(value);
    }

    @Override
    public int getConnectTimeoutMillis() {
        return this.connectTimeoutMillis;
    }

    @Override
    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        if (connectTimeoutMillis < 0) {
            throw new IllegalArgumentException(String.format("connectTimeoutMillis: %d (expected: >= 0)", connectTimeoutMillis));
        }
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    @Override
    public int getWriteSpinCount() {
        return this.writeSpinCount;
    }

    @Override
    public void setWriteSpinCount(int writeSpinCount) {
        if (writeSpinCount <= 0) {
            throw new IllegalArgumentException("writeSpinCount must be a positive integer.");
        }
        this.writeSpinCount = writeSpinCount;
    }

    @Override
    public ByteBufAllocator getAllocator() {
        return this.allocator;
    }

    @Override
    public ByteBufAllocator setAllocator(ByteBufAllocator allocator) {
        if (allocator == null) {
            throw new NullPointerException("allocator");
        }
        ByteBufAllocator oldAllocator = this.allocator;
        this.allocator = allocator;
        return oldAllocator;
    }
}

