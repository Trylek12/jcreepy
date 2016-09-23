
package io.netty.channel.socket.aio;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import java.nio.channels.CompletionHandler;

abstract class AioCompletionHandler<V, A extends Channel>
implements CompletionHandler<V, A> {
    private static final int MAX_STACK_DEPTH = 4;
    private static final ThreadLocal<Integer> STACK_DEPTH = new ThreadLocal<Integer>(){

        @Override
        protected Integer initialValue() {
            return 0;
        }
    };

    AioCompletionHandler() {
    }

    protected abstract void completed0(V var1, A var2);

    protected abstract void failed0(Throwable var1, A var2);

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public final void completed(final V result, A channel) {
        Integer d;
        EventLoop loop = channel.eventLoop();
        if (loop.inEventLoop() && (d = STACK_DEPTH.get()) < 4) {
            STACK_DEPTH.set(d + 1);
            try {
                this.completed0(result, channel);
            }
            finally {
                STACK_DEPTH.set(d);
            }
            return;
        }
        loop.execute(new Runnable((Channel)channel){
            final /* synthetic */ Channel val$channel;

            @Override
            public void run() {
                AioCompletionHandler.this.completed0(result, this.val$channel);
            }
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public final void failed(final Throwable exc, A channel) {
        Integer d;
        EventLoop loop = channel.eventLoop();
        if (loop.inEventLoop() && (d = STACK_DEPTH.get()) < 4) {
            STACK_DEPTH.set(d + 1);
            try {
                this.failed0(exc, channel);
            }
            finally {
                STACK_DEPTH.set(d);
            }
            return;
        }
        loop.execute(new Runnable((Channel)channel){
            final /* synthetic */ Channel val$channel;

            @Override
            public void run() {
                AioCompletionHandler.this.failed0(exc, this.val$channel);
            }
        });
    }

}

