
package io.netty.channel.socket;

import com.sun.nio.sctp.SctpStandardSocketOptions;
import io.netty.channel.ChannelConfig;

public interface SctpServerChannelConfig
extends ChannelConfig {
    public int getBacklog();

    public void setBacklog(int var1);

    public int getSendBufferSize();

    public void setSendBufferSize(int var1);

    public int getReceiveBufferSize();

    public void setReceiveBufferSize(int var1);

    public SctpStandardSocketOptions.InitMaxStreams getInitMaxStreams();

    public void setInitMaxStreams(SctpStandardSocketOptions.InitMaxStreams var1);
}

