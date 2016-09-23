
package io.netty.channel.socket.nio;

import io.netty.channel.ChannelException;
import io.netty.channel.socket.DefaultDatagramChannelConfig;
import io.netty.util.internal.DetectionUtil;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.NetworkChannel;
import java.util.Enumeration;

class NioDatagramChannelConfig
extends DefaultDatagramChannelConfig {
    private static final Object IP_MULTICAST_TTL;
    private static final Object IP_MULTICAST_IF;
    private static final Object IP_MULTICAST_LOOP;
    private static final Method GET_OPTION;
    private static final Method SET_OPTION;
    private final DatagramChannel channel;

    NioDatagramChannelConfig(DatagramChannel channel) {
        super(channel.socket());
        this.channel = channel;
    }

    @Override
    public int getTimeToLive() {
        return (Integer)this.getOption0(IP_MULTICAST_TTL);
    }

    @Override
    public void setTimeToLive(int ttl) {
        this.setOption0(IP_MULTICAST_TTL, ttl);
    }

    @Override
    public InetAddress getInterface() {
        NetworkInterface inf = this.getNetworkInterface();
        if (inf == null) {
            return null;
        }
        Enumeration<InetAddress> addresses = inf.getInetAddresses();
        if (addresses.hasMoreElements()) {
            return addresses.nextElement();
        }
        return null;
    }

    @Override
    public void setInterface(InetAddress interfaceAddress) {
        try {
            this.setNetworkInterface(NetworkInterface.getByInetAddress(interfaceAddress));
        }
        catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public NetworkInterface getNetworkInterface() {
        return (NetworkInterface)this.getOption0(IP_MULTICAST_IF);
    }

    @Override
    public void setNetworkInterface(NetworkInterface networkInterface) {
        this.setOption0(IP_MULTICAST_IF, networkInterface);
    }

    @Override
    public boolean isLoopbackModeDisabled() {
        return (Boolean)this.getOption0(IP_MULTICAST_LOOP);
    }

    @Override
    public void setLoopbackModeDisabled(boolean loopbackModeDisabled) {
        this.setOption0(IP_MULTICAST_LOOP, loopbackModeDisabled);
    }

    private Object getOption0(Object option) {
        if (DetectionUtil.javaVersion() < 7) {
            throw new UnsupportedOperationException();
        }
        try {
            return GET_OPTION.invoke(this.channel, option);
        }
        catch (Exception e) {
            throw new ChannelException(e);
        }
    }

    private void setOption0(Object option, Object value) {
        if (DetectionUtil.javaVersion() < 7) {
            throw new UnsupportedOperationException();
        }
        try {
            SET_OPTION.invoke(this.channel, option, value);
        }
        catch (Exception e) {
            throw new ChannelException(e);
        }
    }

    static {
        ClassLoader classLoader = DatagramChannel.class.getClassLoader();
        Class socketOptionType = null;
        try {
            socketOptionType = Class.forName("java.net.SocketOption", true, classLoader);
        }
        catch (Exception e) {
            // empty catch block
        }
        Class stdSocketOptionType = null;
        try {
            stdSocketOptionType = Class.forName("java.net.StandardSocketOptions", true, classLoader);
        }
        catch (Exception e) {
            // empty catch block
        }
        Object ipMulticastTtl = null;
        Object ipMulticastIf = null;
        Object ipMulticastLoop = null;
        Method getOption = null;
        Method setOption = null;
        if (socketOptionType != null) {
            try {
                ipMulticastTtl = stdSocketOptionType.getDeclaredField("IP_MULTICAST_TTL").get(null);
            }
            catch (Exception e) {
                throw new Error("cannot locate the IP_MULTICAST_TTL field", e);
            }
            try {
                ipMulticastIf = stdSocketOptionType.getDeclaredField("IP_MULTICAST_IF").get(null);
            }
            catch (Exception e) {
                throw new Error("cannot locate the IP_MULTICAST_IF field", e);
            }
            try {
                ipMulticastLoop = stdSocketOptionType.getDeclaredField("IP_MULTICAST_LOOP").get(null);
            }
            catch (Exception e) {
                throw new Error("cannot locate the IP_MULTICAST_LOOP field", e);
            }
            try {
                getOption = NetworkChannel.class.getDeclaredMethod("getOption", socketOptionType);
            }
            catch (Exception e) {
                throw new Error("cannot locate the getOption() method", e);
            }
            try {
                setOption = NetworkChannel.class.getDeclaredMethod("setOption", socketOptionType, Object.class);
            }
            catch (Exception e) {
                throw new Error("cannot locate the setOption() method", e);
            }
        }
        IP_MULTICAST_TTL = ipMulticastTtl;
        IP_MULTICAST_IF = ipMulticastIf;
        IP_MULTICAST_LOOP = ipMulticastLoop;
        GET_OPTION = getOption;
        SET_OPTION = setOption;
    }
}

