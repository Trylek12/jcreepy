
package io.netty.channel;

import io.netty.channel.EventExecutor;
import io.netty.channel.EventLoopGroup;

public interface EventLoop
extends EventExecutor,
EventLoopGroup {
    @Override
    public EventLoopGroup parent();
}

