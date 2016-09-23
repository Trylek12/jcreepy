
package io.netty.channel.socket.oio;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ChannelBufType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.EventLoop;
import io.netty.channel.FileRegion;
import io.netty.channel.socket.DefaultSocketChannelConfig;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.SocketChannelConfig;
import io.netty.channel.socket.oio.AbstractOioByteChannel;
import io.netty.channel.socket.oio.AbstractOioChannel;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.channels.Channels;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.WritableByteChannel;

public class OioSocketChannel
extends AbstractOioByteChannel
implements SocketChannel {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(OioSocketChannel.class);
    private static final ChannelMetadata METADATA = new ChannelMetadata(ChannelBufType.BYTE, false);
    private final Socket socket;
    private final SocketChannelConfig config;
    private InputStream is;
    private OutputStream os;
    private WritableByteChannel outChannel;

    public OioSocketChannel() {
        this(new Socket());
    }

    public OioSocketChannel(Socket socket) {
        this(null, null, socket);
    }

    public OioSocketChannel(Channel parent, Integer id, Socket socket) {
        super(parent, id);
        this.socket = socket;
        this.config = new DefaultSocketChannelConfig(socket);
        boolean success = false;
        try {
            if (socket.isConnected()) {
                this.is = socket.getInputStream();
                this.os = socket.getOutputStream();
            }
            socket.setSoTimeout(1000);
            success = true;
        }
        catch (Exception e) {
            throw new ChannelException("failed to initialize a socket", e);
        }
        finally {
            if (!success) {
                try {
                    socket.close();
                }
                catch (IOException e) {
                    logger.warn("Failed to close a socket.", e);
                }
            }
        }
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
    public boolean isOpen() {
        return !this.socket.isClosed();
    }

    @Override
    public boolean isActive() {
        return !this.socket.isClosed() && this.socket.isConnected();
    }

    @Override
    public boolean isInputShutdown() {
        return super.isInputShutdown();
    }

    @Override
    public boolean isOutputShutdown() {
        return this.socket.isOutputShutdown() || !this.isActive();
    }

    @Override
    public ChannelFuture shutdownOutput() {
        final ChannelFuture future = this.newFuture();
        EventLoop loop = this.eventLoop();
        if (loop.inEventLoop()) {
            this.shutdownOutput(future);
        } else {
            loop.execute(new Runnable(){

                @Override
                public void run() {
                    OioSocketChannel.this.shutdownOutput(future);
                }
            });
        }
        return future;
    }

    private void shutdownOutput(ChannelFuture future) {
        try {
            this.socket.shutdownOutput();
            future.setSuccess();
        }
        catch (Throwable t) {
            future.setFailure(t);
        }
    }

    @Override
    protected SocketAddress localAddress0() {
        return this.socket.getLocalSocketAddress();
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return this.socket.getRemoteSocketAddress();
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        this.socket.bind(localAddress);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected void doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        if (localAddress != null) {
            this.socket.bind(localAddress);
        }
        boolean success = false;
        try {
            this.socket.connect(remoteAddress, this.config().getConnectTimeoutMillis());
            this.is = this.socket.getInputStream();
            this.os = this.socket.getOutputStream();
            success = true;
        }
        finally {
            if (!success) {
                this.doClose();
            }
        }
    }

    @Override
    protected void doDisconnect() throws Exception {
        this.doClose();
    }

    @Override
    protected void doClose() throws Exception {
        this.socket.close();
    }

    @Override
    protected int available() {
        try {
            return this.is.available();
        }
        catch (IOException e) {
            return 0;
        }
    }

    @Override
    protected int doReadBytes(ByteBuf buf) throws Exception {
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
        try {
            return buf.writeBytes(this.is, buf.writableBytes());
        }
        catch (SocketTimeoutException e) {
            return 0;
        }
    }

    @Override
    protected void doWriteBytes(ByteBuf buf) throws Exception {
        OutputStream os = this.os;
        if (os == null) {
            throw new NotYetConnectedException();
        }
        buf.readBytes(os, buf.readableBytes());
    }

    @Override
    protected void doFlushFileRegion(FileRegion region, ChannelFuture future) throws Exception {
        long localWritten;
        OutputStream os = this.os;
        if (os == null) {
            throw new NotYetConnectedException();
        }
        if (this.outChannel == null) {
            this.outChannel = Channels.newChannel(os);
        }
        long written = 0;
        do {
            if ((localWritten = region.transferTo(this.outChannel, written)) != -1) continue;
            OioSocketChannel.checkEOF(region, written);
            region.close();
            future.setSuccess();
            return;
        } while ((written += localWritten) < region.count());
        future.setSuccess();
    }

}

