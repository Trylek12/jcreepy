
package io.netty.channel.socket;

import io.netty.channel.ChannelException;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.socket.SocketChannelConfig;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;

public class DefaultSocketChannelConfig
extends DefaultChannelConfig
implements SocketChannelConfig {
    private final Socket socket;
    private volatile boolean allowHalfClosure;

    public DefaultSocketChannelConfig(Socket socket) {
        if (socket == null) {
            throw new NullPointerException("socket");
        }
        this.socket = socket;
    }

    @Override
    public Map<ChannelOption<?>, Object> getOptions() {
        return this.getOptions(super.getOptions(), ChannelOption.SO_RCVBUF, ChannelOption.SO_SNDBUF, ChannelOption.TCP_NODELAY, ChannelOption.SO_KEEPALIVE, ChannelOption.SO_REUSEADDR, ChannelOption.SO_LINGER, ChannelOption.IP_TOS, ChannelOption.ALLOW_HALF_CLOSURE);
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
            return this.socket.getReceiveBufferSize();
        }
        catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public int getSendBufferSize() {
        try {
            return this.socket.getSendBufferSize();
        }
        catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public int getSoLinger() {
        try {
            return this.socket.getSoLinger();
        }
        catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public int getTrafficClass() {
        try {
            return this.socket.getTrafficClass();
        }
        catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public boolean isKeepAlive() {
        try {
            return this.socket.getKeepAlive();
        }
        catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public boolean isReuseAddress() {
        try {
            return this.socket.getReuseAddress();
        }
        catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public boolean isTcpNoDelay() {
        try {
            return this.socket.getTcpNoDelay();
        }
        catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public void setKeepAlive(boolean keepAlive) {
        try {
            this.socket.setKeepAlive(keepAlive);
        }
        catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        this.socket.setPerformancePreferences(connectionTime, latency, bandwidth);
    }

    @Override
    public void setReceiveBufferSize(int receiveBufferSize) {
        try {
            this.socket.setReceiveBufferSize(receiveBufferSize);
        }
        catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public void setReuseAddress(boolean reuseAddress) {
        try {
            this.socket.setReuseAddress(reuseAddress);
        }
        catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public void setSendBufferSize(int sendBufferSize) {
        try {
            this.socket.setSendBufferSize(sendBufferSize);
        }
        catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public void setSoLinger(int soLinger) {
        try {
            if (soLinger < 0) {
                this.socket.setSoLinger(false, 0);
            } else {
                this.socket.setSoLinger(true, soLinger);
            }
        }
        catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public void setTcpNoDelay(boolean tcpNoDelay) {
        try {
            this.socket.setTcpNoDelay(tcpNoDelay);
        }
        catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public void setTrafficClass(int trafficClass) {
        try {
            this.socket.setTrafficClass(trafficClass);
        }
        catch (SocketException e) {
            throw new ChannelException(e);
        }
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

