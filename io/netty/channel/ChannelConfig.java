
package io.netty.channel;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import java.util.Map;

public interface ChannelConfig {
    public Map<ChannelOption<?>, Object> getOptions();

    public boolean setOptions(Map<ChannelOption<?>, ?> var1);

    public <T> T getOption(ChannelOption<T> var1);

    public <T> boolean setOption(ChannelOption<T> var1, T var2);

    public int getConnectTimeoutMillis();

    public void setConnectTimeoutMillis(int var1);

    public int getWriteSpinCount();

    public void setWriteSpinCount(int var1);

    public ByteBufAllocator getAllocator();

    public ByteBufAllocator setAllocator(ByteBufAllocator var1);
}

