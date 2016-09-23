
package io.netty.channel.socket;

import com.sun.nio.sctp.SctpStandardSocketOptions;
import io.netty.channel.ChannelConfig;

public interface SctpChannelConfig
extends ChannelConfig {
    public boolean isSctpNoDelay();

    public void setSctpNoDelay(boolean var1);

    public int getSendBufferSize();

    public void setSendBufferSize(int var1);

    public int getReceiveBufferSize();

    public void setReceiveBufferSize(int var1);

    public SctpStandardSocketOptions.InitMaxStreams getInitMaxStreams();

    public void setInitMaxStreams(SctpStandardSocketOptions.InitMaxStreams var1);
}

