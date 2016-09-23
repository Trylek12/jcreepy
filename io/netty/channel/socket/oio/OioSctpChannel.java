
package io.netty.channel.socket.oio;

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
import io.netty.channel.EventLoop;
import io.netty.channel.socket.DefaultSctpChannelConfig;
import io.netty.channel.socket.SctpChannel;
import io.netty.channel.socket.SctpChannelConfig;
import io.netty.channel.socket.SctpMessage;
import io.netty.channel.socket.SctpNotificationHandler;
import io.netty.channel.socket.oio.AbstractOioChannel;
import io.netty.channel.socket.oio.AbstractOioMessageChannel;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class OioSctpChannel
extends AbstractOioMessageChannel
implements SctpChannel {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(OioSctpChannel.class);
    private static final ChannelMetadata METADATA = new ChannelMetadata(ChannelBufType.MESSAGE, false);
    private final com.sun.nio.sctp.SctpChannel ch;
    private final SctpChannelConfig config;
    private final NotificationHandler<?> notificationHandler;

    private static com.sun.nio.sctp.SctpChannel openChannel() {
        try {
            return com.sun.nio.sctp.SctpChannel.open();
        }
        catch (IOException e) {
            throw new ChannelException("Failed to open a sctp channel.", e);
        }
    }

    public OioSctpChannel() {
        this(OioSctpChannel.openChannel());
    }

    public OioSctpChannel(com.sun.nio.sctp.SctpChannel ch) {
        this(null, null, ch);
    }

    public OioSctpChannel(Channel parent, Integer id, com.sun.nio.sctp.SctpChannel ch) {
        super(parent, id);
        this.ch = ch;
        boolean success = false;
        try {
            ch.configureBlocking(true);
            this.config = new DefaultSctpChannelConfig(ch);
            this.notificationHandler = new SctpNotificationHandler(this);
            success = true;
        }
        catch (Exception e) {
            throw new ChannelException("failed to initialize a sctp channel", e);
        }
        finally {
            if (!success) {
                try {
                    ch.close();
                }
                catch (IOException e) {
                    logger.warn("Failed to close a sctp channel.", e);
                }
            }
        }
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    public SctpChannelConfig config() {
        return this.config;
    }

    @Override
    public boolean isOpen() {
        return this.ch.isOpen();
    }

    @Override
    protected int doReadMessages(MessageBuf<Object> buf) throws Exception {
        if (this.readSuspended) {
            return 0;
        }
        ByteBuffer data = ByteBuffer.allocate(this.config().getReceiveBufferSize());
        MessageInfo messageInfo = this.ch.receive(data, null, this.notificationHandler);
        if (messageInfo == null) {
            return 0;
        }
        data.flip();
        buf.add(new SctpMessage(messageInfo, Unpooled.wrappedBuffer(data)));
        if (this.readSuspended) {
            return 0;
        }
        return 1;
    }

    @Override
    protected void doWriteMessages(MessageBuf<Object> buf) throws Exception {
        ByteBuffer nioData;
        SctpMessage packet = (SctpMessage)buf.poll();
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
        this.ch.send(nioData, mi);
    }

    @Override
    public Association association() {
        try {
            return this.ch.association();
        }
        catch (IOException e) {
            return null;
        }
    }

    @Override
    public boolean isActive() {
        return this.isOpen() && this.association() != null;
    }

    @Override
    protected SocketAddress localAddress0() {
        try {
            Iterator<SocketAddress> i = this.ch.getAllLocalAddresses().iterator();
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
    public Set<SocketAddress> allLocalAddresses() {
        try {
            Set<SocketAddress> allLocalAddresses = this.ch.getAllLocalAddresses();
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
    protected SocketAddress remoteAddress0() {
        try {
            Iterator<SocketAddress> i = this.ch.getRemoteAddresses().iterator();
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
    public Set<SocketAddress> allRemoteAddresses() {
        try {
            Set<SocketAddress> allLocalAddresses = this.ch.getRemoteAddresses();
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
    protected void doBind(SocketAddress localAddress) throws Exception {
        this.ch.bind(localAddress);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected void doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        if (localAddress != null) {
            this.ch.bind(localAddress);
        }
        boolean success = false;
        try {
            this.ch.connect(remoteAddress);
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
        this.ch.close();
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
                this.ch.bindAddress(localAddress);
                future.setSuccess();
            }
            catch (Throwable t) {
                future.setFailure(t);
            }
        } else {
            this.eventLoop().execute(new Runnable(){

                @Override
                public void run() {
                    OioSctpChannel.this.doBindAddress(localAddress, future);
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
                this.ch.unbindAddress(localAddress);
                future.setSuccess();
            }
            catch (Throwable t) {
                future.setFailure(t);
            }
        } else {
            this.eventLoop().execute(new Runnable(){

                @Override
                public void run() {
                    OioSctpChannel.this.doUnbindAddress(localAddress, future);
                }
            });
        }
    }

}

