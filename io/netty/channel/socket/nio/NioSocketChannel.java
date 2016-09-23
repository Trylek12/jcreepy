
package io.netty.channel.socket.nio;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ChannelBufType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.socket.DefaultSocketChannelConfig;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.SocketChannelConfig;
import io.netty.channel.socket.nio.AbstractNioByteChannel;
import io.netty.channel.socket.nio.NioEventLoop;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

public class NioSocketChannel
extends AbstractNioByteChannel
implements SocketChannel {
    private static final ChannelMetadata METADATA = new ChannelMetadata(ChannelBufType.BYTE, false);
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NioSocketChannel.class);
    private final SocketChannelConfig config;

    private static java.nio.channels.SocketChannel newSocket() {
        try {
            return java.nio.channels.SocketChannel.open();
        }
        catch (IOException e) {
            throw new ChannelException("Failed to open a socket.", e);
        }
    }

    public NioSocketChannel() {
        this(NioSocketChannel.newSocket());
    }

    public NioSocketChannel(java.nio.channels.SocketChannel socket) {
        this(null, null, socket);
    }

    public NioSocketChannel(Channel parent, Integer id, java.nio.channels.SocketChannel socket) {
        super(parent, id, socket);
        try {
            socket.configureBlocking(false);
        }
        catch (IOException e) {
            block4 : {
                try {
                    socket.close();
                }
                catch (IOException e2) {
                    if (!logger.isWarnEnabled()) break block4;
                    logger.warn("Failed to close a partially initialized socket.", e2);
                }
            }
            throw new ChannelException("Failed to enter non-blocking mode.", e);
        }
        this.config = new DefaultSocketChannelConfig(socket.socket());
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    public SocketChannelConfig config() {
        return this.config;
    }

    @Override
    protected java.nio.channels.SocketChannel javaChannel() {
        return (java.nio.channels.SocketChannel)super.javaChannel();
    }

    @Override
    public boolean isActive() {
        java.nio.channels.SocketChannel ch = this.javaChannel();
        return ch.isOpen() && ch.isConnected();
    }

    @Override
    public boolean isInputShutdown() {
        return super.isInputShutdown();
    }

    @Override
    public boolean isOutputShutdown() {
        return this.javaChannel().socket().isOutputShutdown() || !this.isActive();
    }

    @Override
    public ChannelFuture shutdownOutput() {
        final ChannelFuture future = this.newFuture();
        NioEventLoop loop = this.eventLoop();
        if (loop.inEventLoop()) {
            this.shutdownOutput(future);
        } else {
            loop.execute(new Runnable(){

                @Override
                public void run() {
                    NioSocketChannel.this.shutdownOutput(future);
                }
            });
        }
        return future;
    }

    private void shutdownOutput(ChannelFuture future) {
        try {
            this.javaChannel().socket().shutdownOutput();
            future.setSuccess();
        }
        catch (Throwable t) {
            future.setFailure(t);
        }
    }

    @Override
    protected SocketAddress localAddress0() {
        return this.javaChannel().socket().getLocalSocketAddress();
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return this.javaChannel().socket().getRemoteSocketAddress();
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        this.javaChannel().socket().bind(localAddress);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        if (localAddress != null) {
            this.javaChannel().socket().bind(localAddress);
        }
        boolean success = false;
        try {
            boolean connected = this.javaChannel().connect(remoteAddress);
            if (connected) {
                this.selectionKey().interestOps(1);
            } else {
                this.selectionKey().interestOps(8);
            }
            success = true;
            boolean bl = connected;
            return bl;
        }
        finally {
            if (!success) {
                this.doClose();
            }
        }
    }

    @Override
    protected void doFinishConnect() throws Exception {
        if (!this.javaChannel().finishConnect()) {
            throw new Error();
        }
        this.selectionKey().interestOps(1);
    }

    @Override
    protected void doDisconnect() throws Exception {
        this.doClose();
    }

    @Override
    protected void doClose() throws Exception {
        this.javaChannel().close();
    }

    @Override
    protected int doReadBytes(ByteBuf byteBuf) throws Exception {
        return byteBuf.writeBytes(this.javaChannel(), byteBuf.writableBytes());
    }

    @Override
    protected int doWriteBytes(ByteBuf buf, boolean lastSpin) throws Exception {
        int expectedWrittenBytes = buf.readableBytes();
        int writtenBytes = buf.readBytes(this.javaChannel(), expectedWrittenBytes);
        SelectionKey key = this.selectionKey();
        int interestOps = key.interestOps();
        if (writtenBytes >= expectedWrittenBytes) {
            if ((interestOps & 4) != 0) {
                key.interestOps(interestOps & -5);
            }
        } else if ((writtenBytes > 0 || lastSpin) && (interestOps & 4) == 0) {
            key.interestOps(interestOps | 4);
        }
        return writtenBytes;
    }

}

