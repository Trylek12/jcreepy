
package io.netty.channel.socket.aio;

import io.netty.channel.ChannelException;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.socket.SocketChannelConfig;
import java.io.IOException;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.NetworkChannel;
import java.util.Map;

final class AioSocketChannelConfig
extends DefaultChannelConfig
implements SocketChannelConfig {
    private final NetworkChannel channel;
    private volatile boolean allowHalfClosure;
    private volatile long readTimeoutInMillis;
    private volatile long writeTimeoutInMillis;

    AioSocketChannelConfig(NetworkChannel channel) {
        if (channel == null) {
            throw new NullPointerException("channel");
        }
        this.channel = channel;
    }

    @Override
    public Map<ChannelOption<?>, Object> getOptions() {
        return this.getOptions(super.getOptions(), ChannelOption.SO_RCVBUF, ChannelOption.SO_SNDBUF, ChannelOption.TCP_NODELAY, ChannelOption.SO_KEEPALIVE, ChannelOption.SO_REUSEADDR, ChannelOption.SO_LINGER, ChannelOption.IP_TOS, ChannelOption.AIO_READ_TIMEOUT, ChannelOption.AIO_WRITE_TIMEOUT, ChannelOption.ALLOW_HALF_CLOSURE);
    }

    @Override
    public <T> T getOption(ChannelOption<T> option) {
        if (option == ChannelOption.SO_RCVBUF) {
            return this.getReceiveBufferSize();
        }
        if (option == ChannelOption.SO_SNDBUF) {
            return this.getSendBufferSize();
        }
        if (option == ChannelOption.TCP_NODELAY) {
            return this.isTcpNoDelay();
        }
        if (option == ChannelOption.SO_KEEPALIVE) {
            return this.isKeepAlive();
        }
        if (option == ChannelOption.SO_REUSEADDR) {
            return this.isReuseAddress();
        }
        if (option == ChannelOption.SO_LINGER) {
            return this.getSoLinger();
        }
        if (option == ChannelOption.IP_TOS) {
            return this.getTrafficClass();
        }
        if (option == ChannelOption.AIO_READ_TIMEOUT) {
            return this.getReadTimeout();
        }
        if (option == ChannelOption.AIO_WRITE_TIMEOUT) {
            return this.getWriteTimeout();
        }
        if (option == ChannelOption.ALLOW_HALF_CLOSURE) {
            return this.isAllowHalfClosure();
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
        } else if (option == ChannelOption.TCP_NODELAY) {
            this.setTcpNoDelay((Boolean)value);
        } else if (option == ChannelOption.SO_KEEPALIVE) {
            this.setKeepAlive((Boolean)value);
        } else if (option == ChannelOption.SO_REUSEADDR) {
            this.setReuseAddress((Boolean)value);
        } else if (option == ChannelOption.SO_LINGER) {
            this.setSoLinger((Integer)value);
        } else if (option == ChannelOption.IP_TOS) {
            this.setTrafficClass((Integer)value);
        } else if (option == ChannelOption.AIO_READ_TIMEOUT) {
            this.setReadTimeout((Long)value);
        } else if (option == ChannelOption.AIO_WRITE_TIMEOUT) {
            this.setWriteTimeout((Long)value);
        } else if (option == ChannelOption.ALLOW_HALF_CLOSURE) {
            this.setAllowHalfClosure((Boolean)value);
        } else {
            return super.setOption(option, value);
        }
        return true;
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
    public int getSendBufferSize() {
        try {
            return this.channel.getOption(StandardSocketOptions.SO_SNDBUF);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public int getSoLinger() {
        try {
            return this.channel.getOption(StandardSocketOptions.SO_LINGER);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public int getTrafficClass() {
        try {
            return this.channel.getOption(StandardSocketOptions.IP_TOS);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public boolean isKeepAlive() {
        try {
            return this.channel.getOption(StandardSocketOptions.SO_KEEPALIVE);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
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
    public boolean isTcpNoDelay() {
        try {
            return this.channel.getOption(StandardSocketOptions.SO_REUSEADDR);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public void setKeepAlive(boolean keepAlive) {
        try {
            this.channel.setOption(StandardSocketOptions.SO_KEEPALIVE, keepAlive);
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
    public void setReceiveBufferSize(int receiveBufferSize) {
        try {
            this.channel.setOption(StandardSocketOptions.SO_RCVBUF, receiveBufferSize);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public void setReuseAddress(boolean reuseAddress) {
        try {
            this.channel.setOption(StandardSocketOptions.SO_REUSEADDR, reuseAddress);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public void setSendBufferSize(int sendBufferSize) {
        try {
            this.channel.setOption(StandardSocketOptions.SO_SNDBUF, sendBufferSize);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public void setSoLinger(int soLinger) {
        try {
            this.channel.setOption(StandardSocketOptions.SO_LINGER, soLinger);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public void setTcpNoDelay(boolean tcpNoDelay) {
        try {
            this.channel.setOption(StandardSocketOptions.TCP_NODELAY, tcpNoDelay);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public void setTrafficClass(int trafficClass) {
        try {
            this.channel.setOption(StandardSocketOptions.IP_TOS, trafficClass);
        }
        catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    public void setReadTimeout(long readTimeoutInMillis) {
        if (readTimeoutInMillis < 0) {
            throw new IllegalArgumentException("readTimeoutInMillis: " + readTimeoutInMillis);
        }
        this.readTimeoutInMillis = readTimeoutInMillis;
    }

    public void setWriteTimeout(long writeTimeoutInMillis) {
        if (writeTimeoutInMillis < 0) {
            throw new IllegalArgumentException("writeTimeoutInMillis: " + writeTimeoutInMillis);
        }
        this.writeTimeoutInMillis = writeTimeoutInMillis;
    }

    public long getReadTimeout() {
        return this.readTimeoutInMillis;
    }

    public long getWriteTimeout() {
        return this.writeTimeoutInMillis;
    }

    @Override
    public boolean isAllowHalfClosure() {
        return this.allowHalfClosure;
    }

    @Override
    public void setAllowHalfClosure(boolean allowHalfClosure) {
        this.allowHalfClosure = allowHalfClosure;
    }
}

