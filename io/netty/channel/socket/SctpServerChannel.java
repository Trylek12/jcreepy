
package io.netty.channel.socket;

import io.netty.channel.ServerChannel;
import io.netty.channel.socket.SctpServerChannelConfig;
import java.net.SocketAddress;
import java.util.Set;

public interface SctpServerChannel
extends ServerChannel {
    @Override
    public SctpServerChannelConfig config();

    @Override
    public SocketAddress localAddress();

    public Set<SocketAddress> allLocalAddresses();
}

