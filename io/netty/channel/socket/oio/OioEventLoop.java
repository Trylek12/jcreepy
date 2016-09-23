
package io.netty.channel.socket.oio;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelTaskScheduler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.channel.socket.oio.AbstractOioChannel;
import io.netty.channel.socket.oio.OioEventLoopGroup;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

class OioEventLoop
extends SingleThreadEventLoop {
    private final OioEventLoopGroup parent;
    private AbstractOioChannel ch;

    OioEventLoop(OioEventLoopGroup parent) {
        super(parent, parent.threadFactory, parent.scheduler);
        this.parent = parent;
    }

    @Override
    public ChannelFuture register(Channel channel, ChannelFuture future) {
        return super.register(channel, future).addListener(new ChannelFutureListener(){

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    OioEventLoop.this.ch = (AbstractOioChannel)future.channel();
                } else {
                    OioEventLoop.this.deregister();
                }
            }
        });
    }

    /*
     * Unable to fully structure code
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     * Lifted jumps to return sites
     */
    @Override
    protected void run() {
        do lbl-1000: // 3 sources:
        {
            if ((ch = this.ch) == null || !ch.isActive()) {
                try {
                    task = this.takeTask();
                    task.run();
                }
                catch (InterruptedException e) {}
            } else {
                startTime = System.nanoTime();
                while ((task = this.pollTask()) != null) {
                    task.run();
                    if (System.nanoTime() - startTime <= 1000000000) continue;
                }
                ch.unsafe().read();
                if (!ch.isRegistered()) {
                    this.runAllTasks();
                    this.deregister();
                }
            }
            if (!this.isShutdown()) ** GOTO lbl-1000
            if (ch == null) continue;
            ch.unsafe().close(ch.unsafe().voidFuture());
        } while (!this.confirmShutdown());
    }

    private void deregister() {
        this.ch = null;
        this.parent.activeChildren.remove(this);
        this.parent.idleChildren.add(this);
    }

}

