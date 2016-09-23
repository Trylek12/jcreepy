
package io.netty.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventExecutor;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultithreadEventExecutorGroup;
import java.util.concurrent.ThreadFactory;

public abstract class MultithreadEventLoopGroup
extends MultithreadEventExecutorGroup
implements EventLoopGroup {
    protected /* varargs */ MultithreadEventLoopGroup(int nThreads, ThreadFactory threadFactory, Object ... args) {
        super(nThreads, threadFactory, args);
    }

    @Override
    public EventLoop next() {
        return (EventLoop)super.next();
    }

    @Override
    public ChannelFuture register(Channel channel) {
        return this.next().register(channel);
    }

    @Override
    public ChannelFuture register(Channel channel, ChannelFuture future) {
        return this.next().register(channel, future);
    }
}

