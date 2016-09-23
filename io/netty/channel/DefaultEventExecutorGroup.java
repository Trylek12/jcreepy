
package io.netty.channel;

import io.netty.channel.ChannelTaskScheduler;
import io.netty.channel.DefaultEventExecutor;
import io.netty.channel.EventExecutor;
import io.netty.channel.MultithreadEventExecutorGroup;
import java.util.concurrent.ThreadFactory;

public class DefaultEventExecutorGroup
extends MultithreadEventExecutorGroup {
    public DefaultEventExecutorGroup(int nThreads) {
        this(nThreads, null);
    }

    public DefaultEventExecutorGroup(int nThreads, ThreadFactory threadFactory) {
        super(nThreads, threadFactory, new Object[0]);
    }

    @Override
    protected /* varargs */ EventExecutor newChild(ThreadFactory threadFactory, ChannelTaskScheduler scheduler, Object ... args) throws Exception {
        return new DefaultEventExecutor(this, threadFactory, scheduler);
    }
}

