
package io.netty.channel.socket.nio;

import io.netty.buffer.ChannelBufType;
import io.netty.buffer.MessageBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.socket.DefaultServerSocketChannelConfig;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.ServerSocketChannelConfig;
import io.netty.channel.socket.nio.AbstractNioMessageChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class NioServerSocketChannel
extends AbstractNioMessageChannel
implements ServerSocketChannel {
    private static final ChannelMetadata METADATA = new ChannelMetadata(ChannelBufType.MESSAGE, false);
    private final ServerSocketChannelConfig config;

    private static java.nio.channels.ServerSocketChannel newSocket() {
        try {
            return java.nio.channels.ServerSocketChannel.open();
        }
        catch (IOException e) {
            throw new ChannelException("Failed to open a server socket.", e);
        }
    }

    public NioServerSocketChannel() {
        super(null, null, NioServerSocketChannel.newSocket(), 16);
        this.config = new DefaultServerSocketChannelConfig(this.javaChannel().socket());
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    public ServerSocketChannelConfig config() {
        return this.config;
    }

    @Override
    public boolean isActive() {
        return this.javaChannel().socket().isBound();
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return null;
    }

    @Override
    protected java.nio.channels.ServerSocketChannel javaChannel() {
        return (java.nio.channels.ServerSocketChannel)super.javaChannel();
    }

    @Override
    protected SocketAddress localAddress0() {
        return this.javaChannel().socket().getLocalSocketAddress();
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        this.javaChannel().socket().bind(localAddress, this.config.getBacklog());
        SelectionKey selectionKey = this.selectionKey();
        selectionKey.interestOps(selectionKey.interestOps() | 16);
    }

    @Override
    protected void doClose() throws Exception {
        this.javaChannel().close();
    }

    @Override
    protected int doReadMessages(MessageBuf<Object> buf) throws Exception {
        SocketChannel ch = this.javaChannel().accept();
        if (ch == null) {
            return 0;
        }
        buf.add(new NioSocketChannel(this, null, ch));
        return 1;
    }

    @Override
    protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doFinishConnect() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return null;
    }

    @Override
    protected void doDisconnect() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected int doWriteMessages(MessageBuf<Object> buf, boolean lastSpin) throws Exception {
        throw new UnsupportedOperationException();
    }
}

