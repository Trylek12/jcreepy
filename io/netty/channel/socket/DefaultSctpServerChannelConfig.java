
package io.netty.channel.socket;

import com.sun.nio.sctp.SctpServerChannel;
import com.sun.nio.sctp.SctpSocketOption;
import com.sun.nio.sctp.SctpStandardSocketOptions;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.socket.SctpServerChannelConfig;
import io.netty.util.NetworkConstants;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class DefaultSctpServerChannelConfig
extends DefaultChannelConfig
implements SctpServerChannelConfig {
    private final SctpServerChannel serverChannel;
    private volatile int backlog = NetworkConstants.SOMAXCONN;

    public DefaultSctpServerChannelConfig(SctpServerChannel serverChannel) {
        if (serverChannel == null) {
            throw new NullPointerException("serverChannel");
        }
        this.serverChannel = serverChannel;
    }

    @Override
    public Map<ChannelOption<?>, Object> getOptions() {
        return this.getOptions(super.getOptions(), ChannelOption.SO_RCVBUF, ChannelOption.SO_SNDBUF, ChannelOption.SCTP_INIT_MAXSTREAMS);
    }

    @Override
    public <T> T getOption(ChannelOption<T> option) {
        if (option == ChannelOption.SO_RCVBUF) {
            return this.getReceiveBufferSize();
        }
        if (option == ChannelOption.SO_SNDBUF) {
            return this.getSendBufferSize();
        }
        return super.getOption(option);
    }

    @Override
    public <T> boolean setOption(ChannelOption<T> option, T value) {
        this.validate(option, value);
        if (option == ChannelOption.SO_RCVBUF) {
            this.setReceiveBufferSize((Integer)value);
        } else if (option == ChannelOption.SO_SNDBUF) {
            this.setSendBufferSize((Integer)value);
        } else if (option == ChannelOption.SCTP_INIT_MAXSTREAMS) {
            this.setInitMaxStreams((SctpStandardSocketOptions.InitMaxStreams)value);
        } else {
            return super.setOption(option, value);
        }
        return true;
    }

    @Override
    public int getSendBufferSize() {
        try {
            return this.serverChannel.getOption(SctpStandardSocketOptions.SO_SNDBUF);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public void setSendBufferSize(int sendBufferSize) {
        try {
            this.serverChannel.setOption(SctpStandardSocketOptions.SO_SNDBUF, sendBufferSize);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public int getReceiveBufferSize() {
        try {
            return this.serverChannel.getOption(SctpStandardSocketOptions.SO_RCVBUF);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public void setReceiveBufferSize(int receiveBufferSize) {
        try {
            this.serverChannel.setOption(SctpStandardSocketOptions.SO_RCVBUF, receiveBufferSize);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public SctpStandardSocketOptions.InitMaxStreams getInitMaxStreams() {
        try {
            return this.serverChannel.getOption(SctpStandardSocketOptions.SCTP_INIT_MAXSTREAMS);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public void setInitMaxStreams(SctpStandardSocketOptions.InitMaxStreams initMaxStreams) {
        try {
            this.serverChannel.setOption(SctpStandardSocketOptions.SCTP_INIT_MAXSTREAMS, initMaxStreams);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public int getBacklog() {
        return this.backlog;
    }

    @Override
    public void setBacklog(int backlog) {
        if (backlog < 0) {
            throw new IllegalArgumentException("backlog: " + backlog);
        }
        this.backlog = backlog;
    }
}

