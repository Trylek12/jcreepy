
package io.netty.channel.socket.oio;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ChannelBufType;
import io.netty.buffer.MessageBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramChannelConfig;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.DefaultDatagramChannelConfig;
import io.netty.channel.socket.oio.AbstractOioChannel;
import io.netty.channel.socket.oio.AbstractOioMessageChannel;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Locale;

public class OioDatagramChannel
extends AbstractOioMessageChannel
implements DatagramChannel {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(OioDatagramChannel.class);
    private static final ChannelMetadata METADATA = new ChannelMetadata(ChannelBufType.MESSAGE, true);
    private static final byte[] EMPTY_DATA = new byte[0];
    private final MulticastSocket socket;
    private final DatagramChannelConfig config;
    private final java.net.DatagramPacket tmpPacket = new java.net.DatagramPacket(EMPTY_DATA, 0);

    private static MulticastSocket newSocket() {
        try {
            return new MulticastSocket(null);
        }
        catch (Exception e) {
            throw new ChannelException("failed to create a new socket", e);
        }
    }

    public OioDatagramChannel() {
        this(OioDatagramChannel.newSocket());
    }

    public OioDatagramChannel(MulticastSocket socket) {
        this(null, socket);
    }

    public OioDatagramChannel(Integer id, MulticastSocket socket) {
        super(null, id);
        boolean success = false;
        try {
            socket.setSoTimeout(1000);
            socket.setBroadcast(false);
            success = true;
        }
        catch (SocketException e) {
            throw new ChannelException("Failed to configure the datagram socket timeout.", e);
        }
        finally {
            if (!success) {
                socket.close();
            }
        }
        this.socket = socket;
        this.config = new DefaultDatagramChannelConfig(socket);
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    public DatagramChannelConfig config() {
        return this.config;
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
    public boolean isConnected() {
        return this.socket.isConnected();
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
            this.socket.connect(remoteAddress);
            success = true;
        }
        finally {
            if (!success) {
                try {
                    this.socket.close();
                }
                catch (Throwable t) {
                    logger.warn("Failed to close a socket.", t);
                }
            }
        }
    }

    @Override
    protected void doDisconnect() throws Exception {
        this.socket.disconnect();
    }

    @Override
    protected void doClose() throws Exception {
        this.socket.close();
    }

    @Override
    protected int doReadMessages(MessageBuf<Object> buf) throws Exception {
        if (this.readSuspended) {
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {
                // empty catch block
            }
            return 0;
        }
        int packetSize = this.config().getReceivePacketSize();
        byte[] data = new byte[packetSize];
        this.tmpPacket.setData(data);
        try {
            this.socket.receive(this.tmpPacket);
            InetSocketAddress remoteAddr = (InetSocketAddress)this.tmpPacket.getSocketAddress();
            if (remoteAddr == null) {
                remoteAddr = this.remoteAddress();
            }
            buf.add(new DatagramPacket(Unpooled.wrappedBuffer(data, this.tmpPacket.getOffset(), this.tmpPacket.getLength()), remoteAddr));
            if (this.readSuspended) {
                return 0;
            }
            return 1;
        }
        catch (SocketTimeoutException e) {
            return 0;
        }
        catch (SocketException e) {
            if (!e.getMessage().toLowerCase(Locale.US).contains("socket closed")) {
                throw e;
            }
            return -1;
        }
    }

    @Override
    protected void doWriteMessages(MessageBuf<Object> buf) throws Exception {
        DatagramPacket p = (DatagramPacket)buf.poll();
        ByteBuf data = p.data();
        int length = data.readableBytes();
        InetSocketAddress remote = p.remoteAddress();
        if (remote != null) {
            this.tmpPacket.setSocketAddress(remote);
        }
        if (data.hasArray()) {
            this.tmpPacket.setData(data.array(), data.arrayOffset() + data.readerIndex(), length);
        } else {
            byte[] tmp = new byte[length];
            data.getBytes(data.readerIndex(), tmp);
            this.tmpPacket.setData(tmp);
        }
        this.socket.send(this.tmpPacket);
    }

    @Override
    public ChannelFuture joinGroup(InetAddress multicastAddress) {
        return this.joinGroup(multicastAddress, this.newFuture());
    }

    @Override
    public ChannelFuture joinGroup(InetAddress multicastAddress, ChannelFuture future) {
        this.ensureBound();
        try {
            this.socket.joinGroup(multicastAddress);
            future.setSuccess();
        }
        catch (IOException e) {
            future.setFailure(e);
        }
        return future;
    }

    @Override
    public ChannelFuture joinGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface) {
        return this.joinGroup(multicastAddress, networkInterface, this.newFuture());
    }

    @Override
    public ChannelFuture joinGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface, ChannelFuture future) {
        this.ensureBound();
        try {
            this.socket.joinGroup(multicastAddress, networkInterface);
            future.setSuccess();
        }
        catch (IOException e) {
            future.setFailure(e);
        }
        return future;
    }

    @Override
    public ChannelFuture joinGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source) {
        return this.newFailedFuture(new UnsupportedOperationException());
    }

    @Override
    public ChannelFuture joinGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelFuture future) {
        future.setFailure(new UnsupportedOperationException());
        return future;
    }

    private void ensureBound() {
        if (!this.isActive()) {
            throw new IllegalStateException(DatagramChannel.class.getName() + " must be bound to join a group.");
        }
    }

    @Override
    public ChannelFuture leaveGroup(InetAddress multicastAddress) {
        return this.leaveGroup(multicastAddress, this.newFuture());
    }

    @Override
    public ChannelFuture leaveGroup(InetAddress multicastAddress, ChannelFuture future) {
        try {
            this.socket.leaveGroup(multicastAddress);
            future.setSuccess();
        }
        catch (IOException e) {
            future.setFailure(e);
        }
        return future;
    }

    @Override
    public ChannelFuture leaveGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface) {
        return this.leaveGroup(multicastAddress, networkInterface, this.newFuture());
    }

    @Override
    public ChannelFuture leaveGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface, ChannelFuture future) {
        try {
            this.socket.leaveGroup(multicastAddress, networkInterface);
            future.setSuccess();
        }
        catch (IOException e) {
            future.setFailure(e);
        }
        return future;
    }

    @Override
    public ChannelFuture leaveGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source) {
        return this.newFailedFuture(new UnsupportedOperationException());
    }

    @Override
    public ChannelFuture leaveGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelFuture future) {
        future.setFailure(new UnsupportedOperationException());
        return future;
    }

    @Override
    public ChannelFuture block(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress sourceToBlock) {
        return this.newFailedFuture(new UnsupportedOperationException());
    }

    @Override
    public ChannelFuture block(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress sourceToBlock, ChannelFuture future) {
        future.setFailure(new UnsupportedOperationException());
        return future;
    }

    @Override
    public ChannelFuture block(InetAddress multicastAddress, InetAddress sourceToBlock) {
        return this.newFailedFuture(new UnsupportedOperationException());
    }

    @Override
    public ChannelFuture block(InetAddress multicastAddress, InetAddress sourceToBlock, ChannelFuture future) {
        future.setFailure(new UnsupportedOperationException());
        return future;
    }
}

