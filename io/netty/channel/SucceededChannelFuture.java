
package io.netty.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.CompleteChannelFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SucceededChannelFuture
extends CompleteChannelFuture {
    public SucceededChannelFuture(Channel channel) {
        super(channel);
    }

    @Override
    public Throwable cause() {
        return null;
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public ChannelFuture sync() throws InterruptedException {
        return this;
    }

    @Override
    public ChannelFuture syncUninterruptibly() {
        return this;
    }

    @Override
    public Void get() throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }
}

