
package io.netty.channel.socket.aio;

import io.netty.channel.ChannelException;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.socket.ServerSocketChannelConfig;
import io.netty.util.NetworkConstants;
import java.io.IOException;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.NetworkChannel;
import java.util.Map;

final class AioServerSocketChannelConfig
extends DefaultChannelConfig
implements ServerSocketChannelConfig {
    private final AsynchronousServerSocketChannel channel;
    private volatile int backlog = NetworkConstants.SOMAXCONN;

    AioServerSocketChannelConfig(AsynchronousServerSocketChannel channel) {
        this.channel = channel;
    }

    @Override
    public Map<ChannelOption<?>, Object> getOptions() {
        return this.getOptions(super.getOptions(), ChannelOption.SO_RCVBUF, ChannelOption.SO_REUSEADDR, ChannelOption.SO_BACKLOG);
    }

    @Override
    public <T> T getOption(ChannelOption<T> option) {
        if (option == ChannelOption.SO_RCVBUF) {
            return this.getReceiveBufferSize();
        }
        if (option == ChannelOption.SO_REUSEADDR) {
            return this.isReuseAddress();
        }
        if (option == ChannelOption.SO_BACKLOG) {
            return this.getBacklog();
        }
        return super.getOption(option);
    }

    @Override
    public <T> boolean setOption(ChannelOption<T> option, T value) {
        this.validate(option, value);
        if (option == ChannelOption.SO_RCVBUF) {
            this.setReceiveBufferSize((Integer)value);
        } else if (option == ChannelOption.SO_REUSEADDR) {
            this.setReuseAddress((Boolean)value);
        } else if (option == ChannelOption.SO_BACKLOG) {
            this.setBacklog((Integer)value);
        } else {
            return super.setOption(option, value);
        }
        return true;
    }

    @Override
    public boolean isReuseAddress() {
        try {
            return this.channel.getOption(StandardSocketOptions.SO_REUSEADDR);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public void setReuseAddress(boolean reuseAddress) {
        try {
            this.channel.setOption((SocketOption)StandardSocketOptions.SO_REUSEADDR, (Object)reuseAddress);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public int getReceiveBufferSize() {
        try {
            return this.channel.getOption(StandardSocketOptions.SO_RCVBUF);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public void setReceiveBufferSize(int receiveBufferSize) {
        try {
            this.channel.setOption((SocketOption)StandardSocketOptions.SO_RCVBUF, (Object)receiveBufferSize);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        throw new UnsupportedOperationException();
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

