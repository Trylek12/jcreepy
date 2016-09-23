
package io.netty.channel;

import io.netty.channel.ChannelTaskScheduler;
import io.netty.channel.DefaultEventExecutorGroup;
import io.netty.channel.EventExecutorGroup;
import io.netty.channel.SingleThreadEventExecutor;
import java.util.concurrent.ThreadFactory;

class DefaultEventExecutor
extends SingleThreadEventExecutor {
    DefaultEventExecutor(DefaultEventExecutorGroup parent, ThreadFactory threadFactory, ChannelTaskScheduler scheduler) {
        super(parent, threadFactory, scheduler);
    }

    @Override
    protected void run() {
        do {
            try {
                Runnable task = this.takeTask();
                task.run();
                continue;
            }
            catch (InterruptedException e) {
                // empty catch block
            }
        } while (!this.isShutdown() || !this.confirmShutdown());
    }
}

