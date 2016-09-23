
package io.netty.channel.socket;

import io.netty.channel.ChannelConfig;

public interface ServerSocketChannelConfig
extends ChannelConfig {
    public int getBacklog();

    public void setBacklog(int var1);

    public boolean isReuseAddress();

    public void setReuseAddress(boolean var1);

    public int getReceiveBufferSize();

    public void setReceiveBufferSize(int var1);

    public void setPerformancePreferences(int var1, int var2, int var3);
}

