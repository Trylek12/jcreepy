
package io.netty.channel;

import io.netty.channel.ChannelFuture;
import io.netty.channel.FileRegion;
import java.net.SocketAddress;

public interface ChannelOutboundInvoker {
    public ChannelFuture bind(SocketAddress var1);

    public ChannelFuture connect(SocketAddress var1);

    public ChannelFuture connect(SocketAddress var1, SocketAddress var2);

    public ChannelFuture disconnect();

    public ChannelFuture close();

    public ChannelFuture deregister();

    public ChannelFuture flush();

    public ChannelFuture write(Object var1);

    public ChannelFuture sendFile(FileRegion var1);

    public ChannelFuture bind(SocketAddress var1, ChannelFuture var2);

    public ChannelFuture connect(SocketAddress var1, ChannelFuture var2);

    public ChannelFuture connect(SocketAddress var1, SocketAddress var2, ChannelFuture var3);

    public ChannelFuture disconnect(ChannelFuture var1);

    public ChannelFuture close(ChannelFuture var1);

    public ChannelFuture deregister(ChannelFuture var1);

    public ChannelFuture flush(ChannelFuture var1);

    public ChannelFuture write(Object var1, ChannelFuture var2);

    public ChannelFuture sendFile(FileRegion var1, ChannelFuture var2);
}

