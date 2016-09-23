
package io.netty.channel.socket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.SocketChannelConfig;
import java.net.InetSocketAddress;

public interface SocketChannel
extends Channel {
    @Override
    public SocketChannelConfig config();

    @Override
    public InetSocketAddress localAddress();

    @Override
    public InetSocketAddress remoteAddress();

    public boolean isInputShutdown();

    public boolean isOutputShutdown();

    public ChannelFuture shutdownOutput();
}

