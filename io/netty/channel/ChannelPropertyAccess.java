
package io.netty.channel;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;

interface ChannelPropertyAccess {
    public ChannelPipeline pipeline();

    public ByteBufAllocator alloc();

    public ChannelFuture newFuture();

    public ChannelFuture newSucceededFuture();

    public ChannelFuture newFailedFuture(Throwable var1);
}

