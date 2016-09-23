
package io.netty.channel.socket.oio;

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
import io.netty.channel.socket.oio.AbstractOioChannel;
import io.netty.channel.socket.oio.AbstractOioMessageChannel;
import io.netty.channel.socket.oio.OioSctpChannel;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class OioSctpServerChannel
extends AbstractOioMessageChannel
implements SctpServerChannel {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(OioSctpServerChannel.class);
    private static final ChannelMetadata METADATA = new ChannelMetadata(ChannelBufType.MESSAGE, false);
    final com.sun.nio.sctp.SctpServerChannel sch;
    private final SctpServerChannelConfig config;

    private static com.sun.nio.sctp.SctpServerChannel newServerSocket() {
        try {
            return com.sun.nio.sctp.SctpServerChannel.open();
        }
        catch (IOException e) {
            throw new ChannelException("failed to create a sctp server channel", e);
        }
    }

    public OioSctpServerChannel() {
        this(OioSctpServerChannel.newServerSocket());
    }

    public OioSctpServerChannel(com.sun.nio.sctp.SctpServerChannel sch) {
        this(null, sch);
    }

    public OioSctpServerChannel(Integer id, com.sun.nio.sctp.SctpServerChannel sch) {
        super(null, id);
        if (sch == null) {
            throw new NullPointerException("sctp server channel");
        }
        this.sch = sch;
        boolean success = false;
        try {
            sch.configureBlocking(true);
            this.config = new DefaultSctpServerChannelConfig(sch);
            success = true;
        }
        catch (Exception e) {
            throw new ChannelException("failed to initialize a sctp server channel", e);
        }
        finally {
            if (!success) {
                try {
                    sch.close();
                }
                catch (IOException e) {
                    logger.warn("Failed to close a sctp server channel.", e);
                }
            }
        }
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    public SctpServerChannelConfig config() {
        return this.config;
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return null;
    }

    @Override
    public boolean isOpen() {
        return this.sch.isOpen();
    }

    @Override
    protected SocketAddress localAddress0() {
        try {
            Iterator<SocketAddress> i = this.sch.getAllLocalAddresses().iterator();
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
            Set<SocketAddress> allLocalAddresses = this.sch.getAllLocalAddresses();
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
    public boolean isActive() {
        return this.isOpen() && this.localAddress0() != null;
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        this.sch.bind(localAddress, this.config.getBacklog());
    }

    @Override
    protected void doClose() throws Exception {
        this.sch.close();
    }

    @Override
    protected int doReadMessages(MessageBuf<Object> buf) throws Exception {
        block7 : {
            if (!this.isActive()) {
                return -1;
            }
            if (this.readSuspended) {
                return 0;
            }
            SctpChannel s = null;
            try {
                s = this.sch.accept();
                if (s != null) {
                    buf.add(new OioSctpChannel(this, null, s));
                    return 1;
                }
            }
            catch (Throwable t) {
                logger.warn("Failed to create a new channel from an accepted sctp channel.", t);
                if (s == null) break block7;
                try {
                    s.close();
                }
                catch (Throwable t2) {
                    logger.warn("Failed to close a sctp channel.", t2);
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

