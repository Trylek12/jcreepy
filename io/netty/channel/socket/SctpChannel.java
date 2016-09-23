
package io.netty.channel.socket;

import com.sun.nio.sctp.Association;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.SctpChannelConfig;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Set;

public interface SctpChannel
extends Channel {
    public Association association();

    @Override
    public SocketAddress localAddress();

    public Set<SocketAddress> allLocalAddresses();

    @Override
    public SctpChannelConfig config();

    @Override
    public SocketAddress remoteAddress();

    public Set<SocketAddress> allRemoteAddresses();

    public ChannelFuture bindAddress(InetAddress var1);

    public ChannelFuture unbindAddress(InetAddress var1);
}

