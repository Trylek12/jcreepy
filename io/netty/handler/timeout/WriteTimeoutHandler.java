
package io.netty.handler.timeout;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOperationHandlerAdapter;
import io.netty.channel.EventExecutor;
import io.netty.handler.timeout.WriteTimeoutException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class WriteTimeoutHandler
extends ChannelOperationHandlerAdapter {
    private final long timeoutMillis;
    private boolean closed;

    public WriteTimeoutHandler(int timeoutSeconds) {
        this(timeoutSeconds, TimeUnit.SECONDS);
    }

    public WriteTimeoutHandler(long timeout, TimeUnit unit) {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        this.timeoutMillis = timeout <= 0 ? 0 : Math.max(unit.toMillis(timeout), 1);
    }

    @Override
    public void flush(final ChannelHandlerContext ctx, final ChannelFuture future) throws Exception {
        if (this.timeoutMillis > 0) {
            final ScheduledFuture sf = ctx.executor().schedule(new Runnable(){

                @Override
                public void run() {
                    if (future.setFailure(WriteTimeoutException.INSTANCE)) {
                        try {
                            WriteTimeoutHandler.this.writeTimedOut(ctx);
                        }
                        catch (Throwable t) {
                            ctx.fireExceptionCaught(t);
                        }
                    }
                }
            }, this.timeoutMillis, TimeUnit.MILLISECONDS);
            future.addListener(new ChannelFutureListener(){

                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    sf.cancel(false);
                }
            });
        }
        super.flush(ctx, future);
    }

    protected void writeTimedOut(ChannelHandlerContext ctx) throws Exception {
        if (!this.closed) {
            ctx.fireExceptionCaught(WriteTimeoutException.INSTANCE);
            ctx.close();
            this.closed = true;
        }
    }

}

