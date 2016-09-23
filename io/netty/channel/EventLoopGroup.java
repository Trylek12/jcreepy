
package io.netty.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventExecutorGroup;
import io.netty.channel.EventLoop;

public interface EventLoopGroup
extends EventExecutorGroup {
    @Override
    public EventLoop next();

    public ChannelFuture register(Channel var1);

    public ChannelFuture register(Channel var1, ChannelFuture var2);
}

