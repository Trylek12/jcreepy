
package io.netty.channel.group;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroupFuture;
import java.util.Set;

public interface ChannelGroup
extends Set<Channel>,
Comparable<ChannelGroup> {
    public String name();

    public Channel find(Integer var1);

    public ChannelGroupFuture write(Object var1);

    public ChannelGroupFuture disconnect();

    public ChannelGroupFuture close();
}

