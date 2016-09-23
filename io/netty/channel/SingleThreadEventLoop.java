
package io.netty.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelTaskScheduler;
import io.netty.channel.EventExecutor;
import io.netty.channel.EventExecutorGroup;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopException;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SingleThreadEventExecutor;
import java.util.concurrent.ThreadFactory;

public abstract class SingleThreadEventLoop
extends SingleThreadEventExecutor
implements EventLoop {
    protected SingleThreadEventLoop(EventLoopGroup parent, ThreadFactory threadFactory, ChannelTaskScheduler scheduler) {
        super(parent, threadFactory, scheduler);
    }

    @Override
    public EventLoopGroup parent() {
        return (EventLoopGroup)super.parent();
    }

    @Override
    public EventLoop next() {
        return (EventLoop)super.next();
    }

    @Override
    public ChannelFuture register(Channel channel) {
        if (channel == null) {
            throw new NullPointerException("channel");
        }
        return this.register(channel, channel.newFuture());
    }

    @Override
    public ChannelFuture register(final Channel channel, final ChannelFuture future) {
        if (this.isShutdown()) {
            channel.unsafe().closeForcibly();
            future.setFailure(new EventLoopException("cannot register a channel to a shut down loop"));
            return future;
        }
        if (this.inEventLoop()) {
            channel.unsafe().register(this, future);
        } else {
            this.execute(new Runnable(){

                @Override
                public void run() {
                    channel.unsafe().register(SingleThreadEventLoop.this, future);
                }
            });
        }
        return future;
    }

}

