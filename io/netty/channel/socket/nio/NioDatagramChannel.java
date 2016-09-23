
package io.netty.channel.socket.nio;

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
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.channel.socket.nio.AbstractNioMessageChannel;
import io.netty.channel.socket.nio.NioDatagramChannelConfig;
import io.netty.channel.socket.nio.ProtocolFamilyConverter;
import io.netty.util.internal.DetectionUtil;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.MembershipKey;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class NioDatagramChannel
extends AbstractNioMessageChannel
implements DatagramChannel {
    private static final ChannelMetadata METADATA = new ChannelMetadata(ChannelBufType.MESSAGE, true);
    private final DatagramChannelConfig config;
    private final Map<InetAddress, List<MembershipKey>> memberships = new HashMap<InetAddress, List<MembershipKey>>();

    private static java.nio.channels.DatagramChannel newSocket() {
        try {
            return java.nio.channels.DatagramChannel.open();
        }
        catch (IOException e) {
            throw new ChannelException("Failed to open a socket.", e);
        }
    }

    private static java.nio.channels.DatagramChannel newSocket(InternetProtocolFamily ipFamily) {
        if (ipFamily == null) {
            return NioDatagramChannel.newSocket();
        }
        if (DetectionUtil.javaVersion() < 7) {
            throw new UnsupportedOperationException();
        }
        try {
            return java.nio.channels.DatagramChannel.open(ProtocolFamilyConverter.convert(ipFamily));
        }
        catch (IOException e) {
            throw new ChannelException("Failed to open a socket.", e);
        }
    }

    public NioDatagramChannel() {
        this(NioDatagramChannel.newSocket());
    }

    public NioDatagramChannel(InternetProtocolFamily ipFamily) {
        this(NioDatagramChannel.newSocket(ipFamily));
    }

    public NioDatagramChannel(java.nio.channels.DatagramChannel socket) {
        this(null, socket);
    }

    public NioDatagramChannel(Integer id, java.nio.channels.DatagramChannel socket) {
        super(null, id, socket, 1);
        this.config = new NioDatagramChannelConfig(socket);
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
    public boolean isActive() {
        java.nio.channels.DatagramChannel ch = this.javaChannel();
        return ch.isOpen() && ch.socket().isBound();
    }

    @Override
    public boolean isConnected() {
        return this.javaChannel().isConnected();
    }

    @Override
    protected java.nio.channels.DatagramChannel javaChannel() {
        return (java.nio.channels.DatagramChannel)super.javaChannel();
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
        this.selectionKey().interestOps(1);
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
            this.javaChannel().connect(remoteAddress);
            this.selectionKey().interestOps(this.selectionKey().interestOps() | 1);
            success = true;
            boolean bl = true;
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
        throw new Error();
    }

    @Override
    protected void doDisconnect() throws Exception {
        this.javaChannel().disconnect();
    }

    @Override
    protected void doClose() throws Exception {
        this.javaChannel().close();
    }

    @Override
    protected int doReadMessages(MessageBuf<Object> buf) throws Exception {
        ByteBuffer data;
        java.nio.channels.DatagramChannel ch = this.javaChannel();
        InetSocketAddress remoteAddress = (InetSocketAddress)ch.receive(data = ByteBuffer.allocate(this.config().getReceivePacketSize()));
        if (remoteAddress == null) {
            return 0;
        }
        data.flip();
        buf.add(new DatagramPacket(Unpooled.wrappedBuffer(data), remoteAddress));
        return 1;
    }

    @Override
    protected int doWriteMessages(MessageBuf<Object> buf, boolean lastSpin) throws Exception {
        ByteBuffer nioData;
        DatagramPacket packet = (DatagramPacket)buf.peek();
        ByteBuf data = packet.data();
        int dataLen = data.readableBytes();
        if (data.hasNioBuffer()) {
            nioData = data.nioBuffer();
        } else {
            nioData = ByteBuffer.allocate(dataLen);
            data.getBytes(data.readerIndex(), nioData);
            nioData.flip();
        }
        int writtenBytes = this.javaChannel().send(nioData, packet.remoteAddress());
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
    public ChannelFuture joinGroup(InetAddress multicastAddress) {
        return this.joinGroup(multicastAddress, this.newFuture());
    }

    @Override
    public ChannelFuture joinGroup(InetAddress multicastAddress, ChannelFuture future) {
        try {
            return this.joinGroup(multicastAddress, NetworkInterface.getByInetAddress(this.localAddress().getAddress()), null, future);
        }
        catch (SocketException e) {
            future.setFailure(e);
            return future;
        }
    }

    @Override
    public ChannelFuture joinGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface) {
        return this.joinGroup(multicastAddress, networkInterface, this.newFuture());
    }

    @Override
    public ChannelFuture joinGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface, ChannelFuture future) {
        return this.joinGroup(multicastAddress.getAddress(), networkInterface, null, future);
    }

    @Override
    public ChannelFuture joinGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source) {
        return this.joinGroup(multicastAddress, networkInterface, source, this.newFuture());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public ChannelFuture joinGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelFuture future) {
        if (DetectionUtil.javaVersion() >= 7) {
            if (multicastAddress == null) {
                throw new NullPointerException("multicastAddress");
            }
            if (networkInterface == null) {
                throw new NullPointerException("networkInterface");
            }
            try {
                MembershipKey key = source == null ? this.javaChannel().join(multicastAddress, networkInterface) : this.javaChannel().join(multicastAddress, networkInterface, source);
                NioDatagramChannel nioDatagramChannel = this;
                synchronized (nioDatagramChannel) {
                    List<MembershipKey> keys = this.memberships.get(multicastAddress);
                    if (keys == null) {
                        keys = new ArrayList<MembershipKey>();
                        this.memberships.put(multicastAddress, keys);
                    }
                    keys.add(key);
                }
                future.setSuccess();
            }
            catch (Throwable e) {
                future.setFailure(e);
            }
        } else {
            throw new UnsupportedOperationException();
        }
        return future;
    }

    @Override
    public ChannelFuture leaveGroup(InetAddress multicastAddress) {
        return this.leaveGroup(multicastAddress, this.newFuture());
    }

    @Override
    public ChannelFuture leaveGroup(InetAddress multicastAddress, ChannelFuture future) {
        try {
            return this.leaveGroup(multicastAddress, NetworkInterface.getByInetAddress(this.localAddress().getAddress()), null, future);
        }
        catch (SocketException e) {
            future.setFailure(e);
            return future;
        }
    }

    @Override
    public ChannelFuture leaveGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface) {
        return this.leaveGroup(multicastAddress, networkInterface, this.newFuture());
    }

    @Override
    public ChannelFuture leaveGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface, ChannelFuture future) {
        return this.leaveGroup(multicastAddress.getAddress(), networkInterface, null, future);
    }

    @Override
    public ChannelFuture leaveGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source) {
        return this.leaveGroup(multicastAddress, networkInterface, source, this.newFuture());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public ChannelFuture leaveGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelFuture future) {
        if (DetectionUtil.javaVersion() < 7) {
            throw new UnsupportedOperationException();
        }
        if (multicastAddress == null) {
            throw new NullPointerException("multicastAddress");
        }
        if (networkInterface == null) {
            throw new NullPointerException("networkInterface");
        }
        NioDatagramChannel nioDatagramChannel = this;
        synchronized (nioDatagramChannel) {
            List<MembershipKey> keys;
            if (this.memberships != null && (keys = this.memberships.get(multicastAddress)) != null) {
                Iterator<MembershipKey> keyIt = keys.iterator();
                while (keyIt.hasNext()) {
                    MembershipKey key = keyIt.next();
                    if (!networkInterface.equals(key.networkInterface()) || (source != null || key.sourceAddress() != null) && (source == null || !source.equals(key.sourceAddress()))) continue;
                    key.drop();
                    keyIt.remove();
                }
                if (keys.isEmpty()) {
                    this.memberships.remove(multicastAddress);
                }
            }
        }
        future.setSuccess();
        return future;
    }

    @Override
    public ChannelFuture block(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress sourceToBlock) {
        return this.block(multicastAddress, networkInterface, sourceToBlock, this.newFuture());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public ChannelFuture block(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress sourceToBlock, ChannelFuture future) {
        if (DetectionUtil.javaVersion() < 7) {
            throw new UnsupportedOperationException();
        }
        if (multicastAddress == null) {
            throw new NullPointerException("multicastAddress");
        }
        if (sourceToBlock == null) {
            throw new NullPointerException("sourceToBlock");
        }
        if (networkInterface == null) {
            throw new NullPointerException("networkInterface");
        }
        NioDatagramChannel nioDatagramChannel = this;
        synchronized (nioDatagramChannel) {
            if (this.memberships != null) {
                List<MembershipKey> keys = this.memberships.get(multicastAddress);
                for (MembershipKey key : keys) {
                    if (!networkInterface.equals(key.networkInterface())) continue;
                    try {
                        key.block(sourceToBlock);
                    }
                    catch (IOException e) {
                        future.setFailure(e);
                    }
                }
            }
        }
        future.setSuccess();
        return future;
    }

    @Override
    public ChannelFuture block(InetAddress multicastAddress, InetAddress sourceToBlock) {
        return this.block(multicastAddress, sourceToBlock, this.newFuture());
    }

    @Override
    public ChannelFuture block(InetAddress multicastAddress, InetAddress sourceToBlock, ChannelFuture future) {
        try {
            return this.block(multicastAddress, NetworkInterface.getByInetAddress(this.localAddress().getAddress()), sourceToBlock, future);
        }
        catch (SocketException e) {
            future.setFailure(e);
            return future;
        }
    }
}

