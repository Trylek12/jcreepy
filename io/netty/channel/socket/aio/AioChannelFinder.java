
package io.netty.channel.socket.aio;

import io.netty.channel.socket.aio.AbstractAioChannel;

interface AioChannelFinder {
    public AbstractAioChannel findChannel(Runnable var1) throws Exception;
}

