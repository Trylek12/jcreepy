
package io.netty.channel.socket.nio;

import io.netty.channel.ChannelTaskScheduler;
import io.netty.channel.EventExecutor;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.socket.nio.NioEventLoop;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.ThreadFactory;

public class NioEventLoopGroup
extends MultithreadEventLoopGroup {
    public NioEventLoopGroup() {
        this(0);
    }

    public NioEventLoopGroup(int nThreads) {
        this(nThreads, null);
    }

    public NioEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        super(nThreads, threadFactory, new Object[0]);
    }

    public NioEventLoopGroup(int nThreads, ThreadFactory threadFactory, SelectorProvider selectorProvider) {
        super(nThreads, threadFactory, selectorProvider);
    }

    @Override
    protected /* varargs */ EventExecutor newChild(ThreadFactory threadFactory, ChannelTaskScheduler scheduler, Object ... args) throws Exception {
        SelectorProvider selectorProvider = args == null || args.length == 0 || args[0] == null ? SelectorProvider.provider() : (SelectorProvider)args[0];
        return new NioEventLoop(this, threadFactory, scheduler, selectorProvider);
    }
}

