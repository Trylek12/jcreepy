
package io.netty.channel;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

public interface ChannelStateHandler
extends ChannelHandler {
    public void channelRegistered(ChannelHandlerContext var1) throws Exception;

    public void channelUnregistered(ChannelHandlerContext var1) throws Exception;

    public void channelActive(ChannelHandlerContext var1) throws Exception;

    public void channelInactive(ChannelHandlerContext var1) throws Exception;

    public void inboundBufferUpdated(ChannelHandlerContext var1) throws Exception;
}

