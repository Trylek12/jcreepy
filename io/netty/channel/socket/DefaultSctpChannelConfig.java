
package io.netty.channel.socket;

import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpSocketOption;
import com.sun.nio.sctp.SctpStandardSocketOptions;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.socket.SctpChannelConfig;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class DefaultSctpChannelConfig
extends DefaultChannelConfig
implements SctpChannelConfig {
    private final SctpChannel channel;

    public DefaultSctpChannelConfig(SctpChannel channel) {
        if (channel == null) {
            throw new NullPointerException("channel");
        }
        this.channel = channel;
    }

    @Override
    public Map<ChannelOption<?>, Object> getOptions() {
        return this.getOptions(super.getOptions(), ChannelOption.SO_RCVBUF, ChannelOption.SO_SNDBUF, ChannelOption.SCTP_NODELAY, ChannelOption.SCTP_INIT_MAXSTREAMS);
    }

    @Override
    public <T> T getOption(ChannelOption<T> option) {
        if (option == ChannelOption.SO_RCVBUF) {
            return this.getReceiveBufferSize();
        }
        if (option == ChannelOption.SO_SNDBUF) {
            return this.getSendBufferSize();
        }
        if (option == ChannelOption.SCTP_NODELAY) {
            return this.isSctpNoDelay();
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
        } else if (option == ChannelOption.SCTP_NODELAY) {
            this.setSctpNoDelay((Boolean)value);
        } else if (option == ChannelOption.SCTP_INIT_MAXSTREAMS) {
            this.setInitMaxStreams((SctpStandardSocketOptions.InitMaxStreams)value);
        } else {
            return super.setOption(option, value);
        }
        return true;
    }

    @Override
    public boolean isSctpNoDelay() {
        try {
            return this.channel.getOption(SctpStandardSocketOptions.SCTP_NODELAY);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public void setSctpNoDelay(boolean sctpNoDelay) {
        try {
            this.channel.setOption(SctpStandardSocketOptions.SCTP_NODELAY, sctpNoDelay);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public int getSendBufferSize() {
        try {
            return this.channel.getOption(SctpStandardSocketOptions.SO_SNDBUF);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public void setSendBufferSize(int sendBufferSize) {
        try {
            this.channel.setOption(SctpStandardSocketOptions.SO_SNDBUF, sendBufferSize);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public int getReceiveBufferSize() {
        try {
            return this.channel.getOption(SctpStandardSocketOptions.SO_RCVBUF);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public void setReceiveBufferSize(int receiveBufferSize) {
        try {
            this.channel.setOption(SctpStandardSocketOptions.SO_RCVBUF, receiveBufferSize);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public SctpStandardSocketOptions.InitMaxStreams getInitMaxStreams() {
        try {
            return this.channel.getOption(SctpStandardSocketOptions.SCTP_INIT_MAXSTREAMS);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public void setInitMaxStreams(SctpStandardSocketOptions.InitMaxStreams initMaxStreams) {
        try {
            this.channel.setOption(SctpStandardSocketOptions.SCTP_INIT_MAXSTREAMS, initMaxStreams);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }
}

