
package io.netty.channel.local;

import io.netty.channel.ChannelTaskScheduler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.channel.local.LocalEventLoopGroup;
import java.util.concurrent.ThreadFactory;

final class LocalEventLoop
extends SingleThreadEventLoop {
    LocalEventLoop(LocalEventLoopGroup parent, ThreadFactory threadFactory, ChannelTaskScheduler scheduler) {
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

