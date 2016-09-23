
package io.netty.channel.socket.oio;

import io.netty.buffer.ChannelBufType;
import io.netty.buffer.MessageBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.socket.DefaultServerSocketChannelConfig;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.ServerSocketChannelConfig;
import io.netty.channel.socket.oio.AbstractOioChannel;
import io.netty.channel.socket.oio.AbstractOioMessageChannel;
import io.netty.channel.socket.oio.OioSocketChannel;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class OioServerSocketChannel
extends AbstractOioMessageChannel
implements ServerSocketChannel {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(OioServerSocketChannel.class);
    private static final ChannelMetadata METADATA = new ChannelMetadata(ChannelBufType.MESSAGE, false);
    final ServerSocket socket;
    final Lock shutdownLock = new ReentrantLock();
    private final ServerSocketChannelConfig config;

    private static ServerSocket newServerSocket() {
        try {
            return new ServerSocket();
        }
        catch (IOException e) {
            throw new ChannelException("failed to create a server socket", e);
        }
    }

    public OioServerSocketChannel() {
        this(OioServerSocketChannel.newServerSocket());
    }

    public OioServerSocketChannel(ServerSocket socket) {
        this(null, socket);
    }

    public OioServerSocketChannel(Integer id, ServerSocket socket) {
        super(null, id);
        if (socket == null) {
            throw new NullPointerException("socket");
        }
        boolean success = false;
        try {
            socket.setSoTimeout(1000);
            success = true;
        }
        catch (IOException e) {
            throw new ChannelException("Failed to set the server socket timeout.", e);
        }
        finally {
            block11 : {
                if (!success) {
                    try {
                        socket.close();
                    }
                    catch (IOException e) {
                        if (!logger.isWarnEnabled()) break block11;
                        logger.warn("Failed to close a partially initialized socket.", e);
                    }
                }
            }
        }
        this.socket = socket;
        this.config = new DefaultServerSocketChannelConfig(socket);
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
    public InetSocketAddress remoteAddress() {
        return null;
    }

    @Override
    public boolean isOpen() {
        return !this.socket.isClosed();
    }

    @Override
    public boolean isActive() {
        return this.isOpen() && this.socket.isBound();
    }

    @Override
    protected SocketAddress localAddress0() {
        return this.socket.getLocalSocketAddress();
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        this.socket.bind(localAddress, this.config.getBacklog());
    }

    @Override
    protected void doClose() throws Exception {
        this.socket.close();
    }

    @Override
    protected int doReadMessages(MessageBuf<Object> buf) throws Exception {
        block10 : {
            if (this.socket.isClosed()) {
                return -1;
            }
            if (this.readSuspended) {
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {
                    // empty catch block
                }
                return 0;
            }
            Socket s = null;
            try {
                s = this.socket.accept();
                if (s != null) {
                    buf.add(new OioSocketChannel(this, null, s));
                    return 1;
                }
            }
            catch (SocketTimeoutException e) {
            }
            catch (Throwable t) {
                logger.warn("Failed to create a new channel from an accepted socket.", t);
                if (s == null) break block10;
                try {
                    s.close();
                }
                catch (Throwable t2) {
                    logger.warn("Failed to close a socket.", t2);
                }
            }
        }
        return 0;
    }

    @Override
    protected void doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
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
    protected void doWriteMessages(MessageBuf<Object> buf) throws Exception {
        throw new UnsupportedOperationException();
    }
}

