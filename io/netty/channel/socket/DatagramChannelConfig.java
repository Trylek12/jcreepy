
package io.netty.channel.socket;

import io.netty.channel.ChannelConfig;
import java.net.InetAddress;
import java.net.NetworkInterface;

public interface DatagramChannelConfig
extends ChannelConfig {
    public int getSendBufferSize();

    public void setSendBufferSize(int var1);

    public int getReceiveBufferSize();

    public void setReceiveBufferSize(int var1);

    public int getReceivePacketSize();

    public void setReceivePacketSize(int var1);

    public int getTrafficClass();

    public void setTrafficClass(int var1);

    public boolean isReuseAddress();

    public void setReuseAddress(boolean var1);

    public boolean isBroadcast();

    public void setBroadcast(boolean var1);

    public boolean isLoopbackModeDisabled();

    public void setLoopbackModeDisabled(boolean var1);

    public int getTimeToLive();

    public void setTimeToLive(int var1);

    public InetAddress getInterface();

    public void setInterface(InetAddress var1);

    public NetworkInterface getNetworkInterface();

    public void setNetworkInterface(NetworkInterface var1);
}

