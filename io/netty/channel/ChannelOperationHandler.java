
package io.netty.channel;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.FileRegion;
import java.net.SocketAddress;

public interface ChannelOperationHandler
extends ChannelHandler {
    public void bind(ChannelHandlerContext var1, SocketAddress var2, ChannelFuture var3) throws Exception;

    public void connect(ChannelHandlerContext var1, SocketAddress var2, SocketAddress var3, ChannelFuture var4) throws Exception;

    public void disconnect(ChannelHandlerContext var1, ChannelFuture var2) throws Exception;

    public void close(ChannelHandlerContext var1, ChannelFuture var2) throws Exception;

    public void deregister(ChannelHandlerContext var1, ChannelFuture var2) throws Exception;

    public void flush(ChannelHandlerContext var1, ChannelFuture var2) throws Exception;

    public void sendFile(ChannelHandlerContext var1, FileRegion var2, ChannelFuture var3) throws Exception;
}

