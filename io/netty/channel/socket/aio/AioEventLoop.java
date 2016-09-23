
package io.netty.channel.socket.aio;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelTaskScheduler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.channel.socket.aio.AioEventLoopGroup;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

final class AioEventLoop
extends SingleThreadEventLoop {
    private final Set<Channel> channels = Collections.newSetFromMap(new IdentityHashMap());
    private final ChannelFutureListener registrationListener;
    private final ChannelFutureListener deregistrationListener;

    AioEventLoop(AioEventLoopGroup parent, ThreadFactory threadFactory, ChannelTaskScheduler scheduler) {
        super(parent, threadFactory, scheduler);
        this.registrationListener = new ChannelFutureListener(){

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    return;
                }
                Channel ch = future.channel();
                AioEventLoop.this.channels.add(ch);
                ch.closeFuture().addListener(AioEventLoop.this.deregistrationListener);
            }
        };
        this.deregistrationListener = new ChannelFutureListener(){

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                AioEventLoop.this.channels.remove(future.channel());
            }
        };
    }

    @Override
    public ChannelFuture register(Channel channel) {
        return super.register(channel).addListener(this.registrationListener);
    }

    @Override
    public ChannelFuture register(Channel channel, ChannelFuture future) {
        return super.register(channel, future).addListener(this.registrationListener);
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
            try {
                task = this.takeTask();
                task.run();
            }
            catch (InterruptedException e) {
                // empty catch block
            }
            if (!this.isShutdown()) ** GOTO lbl-1000
            this.closeAll();
        } while (!this.confirmShutdown());
    }

    private void closeAll() {
        ArrayList<Channel> channels = new ArrayList<Channel>(this.channels.size());
        for (Channel ch2 : this.channels) {
            channels.add(ch2);
        }
        for (Channel ch2 : channels) {
            ch2.unsafe().close(ch2.unsafe().voidFuture());
        }
    }

}

