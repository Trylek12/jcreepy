
package io.netty.channel.group;

import io.netty.channel.group.ChannelGroupFuture;
import java.util.EventListener;

public interface ChannelGroupFutureListener
extends EventListener {
    public void operationComplete(ChannelGroupFuture var1) throws Exception;
}

