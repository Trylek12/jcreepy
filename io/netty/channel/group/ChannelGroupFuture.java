
package io.netty.channel.group;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFutureListener;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public interface ChannelGroupFuture
extends Iterable<ChannelFuture> {
    public ChannelGroup getGroup();

    public ChannelFuture find(Integer var1);

    public ChannelFuture find(Channel var1);

    public boolean isDone();

    public boolean isCompleteSuccess();

    public boolean isPartialSuccess();

    public boolean isCompleteFailure();

    public boolean isPartialFailure();

    public void addListener(ChannelGroupFutureListener var1);

    public void removeListener(ChannelGroupFutureListener var1);

    public ChannelGroupFuture await() throws InterruptedException;

    public ChannelGroupFuture awaitUninterruptibly();

    public boolean await(long var1, TimeUnit var3) throws InterruptedException;

    public boolean await(long var1) throws InterruptedException;

    public boolean awaitUninterruptibly(long var1, TimeUnit var3);

    public boolean awaitUninterruptibly(long var1);

    @Override
    public Iterator<ChannelFuture> iterator();
}

