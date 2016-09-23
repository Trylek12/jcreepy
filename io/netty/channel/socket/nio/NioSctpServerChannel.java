
package io.netty.channel.socket.nio;

import com.sun.nio.sctp.SctpChannel;
import io.netty.buffer.ChannelBufType;
import io.netty.buffer.MessageBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.socket.DefaultSctpServerChannelConfig;
import io.netty.channel.socket.SctpServerChannel;
import io.netty.channel.socket.SctpServerChannelConfig;
import io.netty.channel.socket.nio.AbstractNioMessageChannel;
import io.netty.channel.socket.nio.NioSctpChannel;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class NioSctpServerChannel
extends AbstractNioMessageChannel
implements SctpServerChannel {
    private static final ChannelMetadata METADATA = new ChannelMetadata(ChannelBufType.MESSAGE, false);
    private final SctpServerChannelConfig config;

    private static com.sun.nio.sctp.SctpServerChannel newSocket() {
        try {
            return com.sun.nio.sctp.SctpServerChannel.open();
        }
        catch (IOException e) {
            throw new ChannelException("Failed to open a server socket.", e);
        }
    }

    public NioSctpServerChannel() {
        super(null, null, NioSctpServerChannel.newSocket(), 16);
        this.config = new DefaultSctpServerChannelConfig(this.javaChannel());
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    public Set<SocketAddress> allLocalAddresses() {
        try {
            Set<SocketAddress> allLocalAddresses = this.javaChannel().getAllLocalAddresses();
            HashSet<SocketAddress> addresses = new HashSet<SocketAddress>(allLocalAddresses.size());
            for (SocketAddress socketAddress : allLocalAddresses) {
                addresses.add(socketAddress);
            }
            return addresses;
        }
        catch (Throwable t) {
            return Collections.emptySet();
        }
    }

    @Override
    public SctpServerChannelConfig config() {
        return this.config;
    }

    @Override
    public boolean isActive() {
        return this.isOpen() && !this.allLocalAddresses().isEmpty();
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return null;
    }

    @Override
    protected com.sun.nio.sctp.SctpServerChannel javaChannel() {
        return (com.sun.nio.sctp.SctpServerChannel)super.javaChannel();
    }

    @Override
    protected SocketAddress localAddress0() {
        try {
            Iterator<SocketAddress> i = this.javaChannel().getAllLocalAddresses().iterator();
            if (i.hasNext()) {
                return i.next();
            }
        }
        catch (IOException e) {
            // empty catch block
        }
        return null;
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        this.javaChannel().bind(localAddress, this.config.getBacklog());
        SelectionKey selectionKey = this.selectionKey();
        selectionKey.interestOps(selectionKey.interestOps() | 16);
    }

    @Override
    protected void doClose() throws Exception {
        this.javaChannel().close();
    }

    @Override
    protected int doReadMessages(MessageBuf<Object> buf) throws Exception {
        SctpChannel ch = this.javaChannel().accept();
        if (ch == null) {
            return 0;
        }
        buf.add(new NioSctpChannel(this, null, ch));
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

