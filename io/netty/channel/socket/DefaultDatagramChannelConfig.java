
package io.netty.channel.socket;

import io.netty.channel.ChannelException;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.socket.DatagramChannelConfig;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import io.netty.util.internal.DetectionUtil;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Map;

public class DefaultDatagramChannelConfig
extends DefaultChannelConfig
implements DatagramChannelConfig {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultDatagramChannelConfig.class);
    private static final int DEFAULT_RECEIVE_PACKET_SIZE = 2048;
    private final DatagramSocket socket;
    private volatile int receivePacketSize = 2048;

    public DefaultDatagramChannelConfig(DatagramSocket socket) {
        if (socket == null) {
            throw new NullPointerException("socket");
        }
        this.socket = socket;
    }

    @Override
    public Map<ChannelOption<?>, Object> getOptions() {
        return this.getOptions(super.getOptions(), ChannelOption.SO_BROADCAST, ChannelOption.SO_RCVBUF, ChannelOption.SO_SNDBUF, ChannelOption.SO_REUSEADDR, ChannelOption.IP_MULTICAST_LOOP_DISABLED, ChannelOption.IP_MULTICAST_ADDR, ChannelOption.IP_MULTICAST_IF, ChannelOption.IP_MULTICAST_TTL, ChannelOption.IP_TOS, ChannelOption.UDP_RECEIVE_PACKET_SIZE);
    }

    @Override
    public <T> T getOption(ChannelOption<T> option) {
        if (option == ChannelOption.SO_BROADCAST) {
            return this.isBroadcast();
        }
        if (option == ChannelOption.SO_RCVBUF) {
            return this.getReceiveBufferSize();
        }
        if (option == ChannelOption.SO_SNDBUF) {
            return this.getSendBufferSize();
        }
        if (option == ChannelOption.UDP_RECEIVE_PACKET_SIZE) {
            return this.getReceivePacketSize();
        }
        if (option == ChannelOption.SO_REUSEADDR) {
            return this.isReuseAddress();
        }
        if (option == ChannelOption.IP_MULTICAST_LOOP_DISABLED) {
            return this.isLoopbackModeDisabled();
        }
        if (option == ChannelOption.IP_MULTICAST_ADDR) {
            InetAddress i = this.getInterface();
            return (T)i;
        }
        if (option == ChannelOption.IP_MULTICAST_IF) {
            NetworkInterface i = this.getNetworkInterface();
            return (T)i;
        }
        if (option == ChannelOption.IP_MULTICAST_TTL) {
            return this.getTimeToLive();
        }
        if (option == ChannelOption.IP_TOS) {
            return this.getTrafficClass();
        }
        return super.getOption(option);
    }

    @Override
    public <T> boolean setOption(ChannelOption<T> option, T value) {
        this.validate(option, value);
        if (option == ChannelOption.SO_BROADCAST) {
            this.setBroadcast((Boolean)value);
        } else if (option == ChannelOption.SO_RCVBUF) {
            this.setReceiveBufferSize((Integer)value);
        } else if (option == ChannelOption.SO_SNDBUF) {
            this.setSendBufferSize((Integer)value);
        } else if (option == ChannelOption.SO_REUSEADDR) {
            this.setReuseAddress((Boolean)value);
        } else if (option == ChannelOption.IP_MULTICAST_LOOP_DISABLED) {
            this.setLoopbackModeDisabled((Boolean)value);
        } else if (option == ChannelOption.IP_MULTICAST_ADDR) {
            this.setInterface((InetAddress)value);
        } else if (option == ChannelOption.IP_MULTICAST_IF) {
            this.setNetworkInterface((NetworkInterface)value);
        } else if (option == ChannelOption.IP_MULTICAST_TTL) {
            this.setTimeToLive((Integer)value);
        } else if (option == ChannelOption.IP_TOS) {
            this.setTrafficClass((Integer)value);
        } else {
            return super.setOption(option, value);
        }
        return true;
    }

    @Override
    public boolean isBroadcast() {
        try {
            return this.socket.getBroadcast();
        }
        catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public void setBroadcast(boolean broadcast) {
        try {
            if (broadcast && !DetectionUtil.isWindows() && !DetectionUtil.isRoot() && !this.socket.getLocalAddress().isAnyLocalAddress()) {
                logger.warn("A non-root user can't receive a broadcast packet if the socket is not bound to a wildcard address; setting the SO_BROADCAST flag anyway as requested on the socket which is bound to " + this.socket.getLocalSocketAddress() + '.');
            }
            this.socket.setBroadcast(broadcast);
        }
        catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public InetAddress getInterface() {
        if (this.socket instanceof MulticastSocket) {
            try {
                return ((MulticastSocket)this.socket).getInterface();
            }
            catch (SocketException e) {
                throw new ChannelException(e);
            }
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public void setInterface(InetAddress interfaceAddress) {
        if (this.socket instanceof MulticastSocket) {
            try {
                ((MulticastSocket)this.socket).setInterface(interfaceAddress);
            }
            catch (SocketException e) {
                throw new ChannelException(e);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public boolean isLoopbackModeDisabled() {
        if (this.socket instanceof MulticastSocket) {
            try {
                return ((MulticastSocket)this.socket).getLoopbackMode();
            }
            catch (SocketException e) {
                throw new ChannelException(e);
            }
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLoopbackModeDisabled(boolean loopbackModeDisabled) {
        if (this.socket instanceof MulticastSocket) {
            try {
                ((MulticastSocket)this.socket).setLoopbackMode(loopbackModeDisabled);
            }
            catch (SocketException e) {
                throw new ChannelException(e);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public NetworkInterface getNetworkInterface() {
        if (this.socket instanceof MulticastSocket) {
            try {
                return ((MulticastSocket)this.socket).getNetworkInterface();
            }
            catch (SocketException e) {
                throw new ChannelException(e);
            }
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public void setNetworkInterface(NetworkInterface networkInterface) {
        if (this.socket instanceof MulticastSocket) {
            try {
                ((MulticastSocket)this.socket).setNetworkInterface(networkInterface);
            }
            catch (SocketException e) {
                throw new ChannelException(e);
            }
        } else {
            throw new UnsupportedOperationException();
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
    public void setReuseAddress(boolean reuseAddress) {
        try {
            this.socket.setReuseAddress(reuseAddress);
        }
        catch (SocketException e) {
            throw new ChannelException(e);
        }
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
    public void setReceiveBufferSize(int receiveBufferSize) {
        try {
            this.socket.setReceiveBufferSize(receiveBufferSize);
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
    public void setSendBufferSize(int sendBufferSize) {
        try {
            this.socket.setSendBufferSize(sendBufferSize);
        }
        catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public int getReceivePacketSize() {
        return this.receivePacketSize;
    }

    @Override
    public void setReceivePacketSize(int receivePacketSize) {
        if (receivePacketSize <= 0) {
            throw new IllegalArgumentException(String.format("receivePacketSize: %d (expected: > 0)", receivePacketSize));
        }
        this.receivePacketSize = receivePacketSize;
    }

    @Override
    public int getTimeToLive() {
        if (this.socket instanceof MulticastSocket) {
            try {
                return ((MulticastSocket)this.socket).getTimeToLive();
            }
            catch (IOException e) {
                throw new ChannelException(e);
            }
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTimeToLive(int ttl) {
        if (this.socket instanceof MulticastSocket) {
            try {
                ((MulticastSocket)this.socket).setTimeToLive(ttl);
            }
            catch (IOException e) {
                throw new ChannelException(e);
            }
        } else {
            throw new UnsupportedOperationException();
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
    public void setTrafficClass(int trafficClass) {
        try {
            this.socket.setTrafficClass(trafficClass);
        }
        catch (SocketException e) {
            throw new ChannelException(e);
        }
    }
}

