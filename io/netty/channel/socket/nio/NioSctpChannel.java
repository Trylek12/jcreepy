
package io.netty.channel.socket.nio;

import com.sun.nio.sctp.Association;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.NotificationHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ChannelBufType;
import io.netty.buffer.MessageBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.socket.DefaultSctpChannelConfig;
import io.netty.channel.socket.SctpChannel;
import io.netty.channel.socket.SctpChannelConfig;
import io.netty.channel.socket.SctpMessage;
import io.netty.channel.socket.SctpNotificationHandler;
import io.netty.channel.socket.nio.AbstractNioMessageChannel;
import io.netty.channel.socket.nio.NioEventLoop;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class NioSctpChannel
extends AbstractNioMessageChannel
implements SctpChannel {
    private static final ChannelMetadata METADATA = new ChannelMetadata(ChannelBufType.MESSAGE, false);
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NioSctpChannel.class);
    private final SctpChannelConfig config;
    private final NotificationHandler notificationHandler;

    private static com.sun.nio.sctp.SctpChannel newSctpChannel() {
        try {
            return com.sun.nio.sctp.SctpChannel.open();
        }
        catch (IOException e) {
            throw new ChannelException("Failed to open a sctp channel.", e);
        }
    }

    public NioSctpChannel() {
        this(NioSctpChannel.newSctpChannel());
    }

    public NioSctpChannel(com.sun.nio.sctp.SctpChannel sctpChannel) {
        this(null, null, sctpChannel);
    }

    public NioSctpChannel(Channel parent, Integer id, com.sun.nio.sctp.SctpChannel sctpChannel) {
        super(parent, id, sctpChannel, 1);
        try {
            sctpChannel.configureBlocking(false);
            this.config = new DefaultSctpChannelConfig(sctpChannel);
            this.notificationHandler = new SctpNotificationHandler(this);
        }
        catch (IOException e) {
            block4 : {
                try {
                    sctpChannel.close();
                }
                catch (IOException e2) {
                    if (!logger.isWarnEnabled()) break block4;
                    logger.warn("Failed to close a partially initialized sctp channel.", e2);
                }
            }
            throw new ChannelException("Failed to enter non-blocking mode.", e);
        }
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    public Association association() {
        try {
            return this.javaChannel().association();
        }
        catch (IOException e) {
            return null;
        }
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
    public SctpChannelConfig config() {
        return this.config;
    }

    @Override
    public Set<SocketAddress> allRemoteAddresses() {
        try {
            Set<SocketAddress> allLocalAddresses = this.javaChannel().getRemoteAddresses();
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
    protected com.sun.nio.sctp.SctpChannel javaChannel() {
        return (com.sun.nio.sctp.SctpChannel)super.javaChannel();
    }

    @Override
    public boolean isActive() {
        com.sun.nio.sctp.SctpChannel ch = this.javaChannel();
        return ch.isOpen() && this.association() != null;
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
    protected SocketAddress remoteAddress0() {
        try {
            Iterator<SocketAddress> i = this.javaChannel().getRemoteAddresses().iterator();
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
        this.javaChannel().bind(localAddress);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        if (localAddress != null) {
            this.javaChannel().bind(localAddress);
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
    protected int doReadMessages(MessageBuf<Object> buf) throws Exception {
        ByteBuffer data;
        com.sun.nio.sctp.SctpChannel ch = this.javaChannel();
        MessageInfo messageInfo = ch.receive(data = ByteBuffer.allocate(this.config().getReceiveBufferSize()), null, this.notificationHandler);
        if (messageInfo == null) {
            return 0;
        }
        data.flip();
        buf.add(new SctpMessage(messageInfo, Unpooled.wrappedBuffer(data)));
        return 1;
    }

    @Override
    protected int doWriteMessages(MessageBuf<Object> buf, boolean lastSpin) throws Exception {
        ByteBuffer nioData;
        SctpMessage packet = (SctpMessage)buf.peek();
        ByteBuf data = packet.getPayloadBuffer();
        int dataLen = data.readableBytes();
        if (data.hasNioBuffer()) {
            nioData = data.nioBuffer();
        } else {
            nioData = ByteBuffer.allocate(dataLen);
            data.getBytes(data.readerIndex(), nioData);
            nioData.flip();
        }
        MessageInfo mi = MessageInfo.createOutgoing(this.association(), null, packet.getStreamIdentifier());
        mi.payloadProtocolID(packet.getProtocolIdentifier());
        mi.streamNumber(packet.getStreamIdentifier());
        int writtenBytes = this.javaChannel().send(nioData, mi);
        SelectionKey key = this.selectionKey();
        int interestOps = key.interestOps();
        if (writtenBytes <= 0 && dataLen > 0) {
            if (lastSpin && (interestOps & 4) == 0) {
                key.interestOps(interestOps | 4);
            }
            return 0;
        }
        buf.remove();
        if (buf.isEmpty() && (interestOps & 4) != 0) {
            key.interestOps(interestOps & -5);
        }
        return 1;
    }

    @Override
    public ChannelFuture bindAddress(InetAddress localAddress) {
        ChannelFuture future = this.newFuture();
        this.doBindAddress(localAddress, future);
        return future;
    }

    void doBindAddress(final InetAddress localAddress, final ChannelFuture future) {
        if (this.eventLoop().inEventLoop()) {
            try {
                this.javaChannel().bindAddress(localAddress);
                future.setSuccess();
            }
            catch (Throwable t) {
                future.setFailure(t);
            }
        } else {
            this.eventLoop().execute(new Runnable(){

                @Override
                public void run() {
                    NioSctpChannel.this.doBindAddress(localAddress, future);
                }
            });
        }
    }

    @Override
    public ChannelFuture unbindAddress(InetAddress localAddress) {
        ChannelFuture future = this.newFuture();
        this.doUnbindAddress(localAddress, future);
        return future;
    }

    void doUnbindAddress(final InetAddress localAddress, final ChannelFuture future) {
        if (this.eventLoop().inEventLoop()) {
            try {
                this.javaChannel().unbindAddress(localAddress);
                future.setSuccess();
            }
            catch (Throwable t) {
                future.setFailure(t);
            }
        } else {
            this.eventLoop().execute(new Runnable(){

                @Override
                public void run() {
                    NioSctpChannel.this.doUnbindAddress(localAddress, future);
                }
            });
        }
    }

}

