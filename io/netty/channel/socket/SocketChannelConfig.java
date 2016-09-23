
package io.netty.channel.socket;

import io.netty.channel.ChannelConfig;

public interface SocketChannelConfig
extends ChannelConfig {
    public boolean isTcpNoDelay();

    public void setTcpNoDelay(boolean var1);

    public int getSoLinger();

    public void setSoLinger(int var1);

    public int getSendBufferSize();

    public void setSendBufferSize(int var1);

    public int getReceiveBufferSize();

    public void setReceiveBufferSize(int var1);

    public boolean isKeepAlive();

    public void setKeepAlive(boolean var1);

    public int getTrafficClass();

    public void setTrafficClass(int var1);

    public boolean isReuseAddress();

    public void setReuseAddress(boolean var1);

    public void setPerformancePreferences(int var1, int var2, int var3);

    public boolean isAllowHalfClosure();

    public void setAllowHalfClosure(boolean var1);
}

