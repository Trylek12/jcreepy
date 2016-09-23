
package io.netty.channel;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

public interface ChannelFutureProgressListener
extends ChannelFutureListener {
    public void operationProgressed(ChannelFuture var1, long var2, long var4, long var6) throws Exception;
}

