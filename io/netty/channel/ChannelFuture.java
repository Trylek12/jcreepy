
package io.netty.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public interface ChannelFuture
extends Future<Void> {
    public Channel channel();

    @Override
    public boolean isDone();

    @Override
    public boolean isCancelled();

    public boolean isSuccess();

    public Throwable cause();

    public boolean cancel();

    public boolean setSuccess();

    public boolean setFailure(Throwable var1);

    public boolean setProgress(long var1, long var3, long var5);

    public ChannelFuture addListener(ChannelFutureListener var1);

    public ChannelFuture removeListener(ChannelFutureListener var1);

    public ChannelFuture sync() throws InterruptedException;

    public ChannelFuture syncUninterruptibly();

    public ChannelFuture await() throws InterruptedException;

    public ChannelFuture awaitUninterruptibly();

    public boolean await(long var1, TimeUnit var3) throws InterruptedException;

    public boolean await(long var1) throws InterruptedException;

    public boolean awaitUninterruptibly(long var1, TimeUnit var3);

    public boolean awaitUninterruptibly(long var1);

    public static interface Unsafe
    extends ChannelFuture {
    }

}

