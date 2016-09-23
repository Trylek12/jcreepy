
package io.netty.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.CompleteChannelFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FailedChannelFuture
extends CompleteChannelFuture {
    private final Throwable cause;

    public FailedChannelFuture(Channel channel, Throwable cause) {
        super(channel);
        if (cause == null) {
            throw new NullPointerException("cause");
        }
        this.cause = cause;
    }

    @Override
    public Throwable cause() {
        return this.cause;
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public ChannelFuture sync() throws InterruptedException {
        return this.rethrow();
    }

    @Override
    public ChannelFuture syncUninterruptibly() {
        return this.rethrow();
    }

    private ChannelFuture rethrow() {
        if (this.cause instanceof RuntimeException) {
            throw (RuntimeException)this.cause;
        }
        if (this.cause instanceof Error) {
            throw (Error)this.cause;
        }
        throw new ChannelException(this.cause);
    }

    @Override
    public Void get() throws InterruptedException, ExecutionException {
        throw new ExecutionException(this.cause);
    }

    @Override
    public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new ExecutionException(this.cause);
    }
}

