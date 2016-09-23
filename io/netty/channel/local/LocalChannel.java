
package io.netty.channel.local;

import io.netty.buffer.ChannelBufType;
import io.netty.buffer.MessageBuf;
import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.EventLoop;
import io.netty.channel.SingleThreadEventExecutor;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannelRegistry;
import io.netty.channel.local.LocalServerChannel;
import java.net.SocketAddress;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.NotYetConnectedException;
import java.util.Collection;
import java.util.Collections;

public class LocalChannel
extends AbstractChannel {
    private static final ChannelMetadata METADATA = new ChannelMetadata(ChannelBufType.MESSAGE, false);
    private final ChannelConfig config = new DefaultChannelConfig();
    private final Runnable shutdownHook;
    private volatile int state;
    private volatile LocalChannel peer;
    private volatile LocalAddress localAddress;
    private volatile LocalAddress remoteAddress;
    private volatile ChannelFuture connectFuture;

    public LocalChannel() {
        this(null);
    }

    public LocalChannel(Integer id) {
        super(null, id);
        this.shutdownHook = new Runnable(){

            @Override
            public void run() {
                LocalChannel.this.unsafe().close(LocalChannel.this.unsafe().voidFuture());
            }
        };
    }

    LocalChannel(LocalServerChannel parent, LocalChannel peer) {
        super(parent, null);
        this.shutdownHook = new ;
        this.peer = peer;
        this.localAddress = parent.localAddress();
        this.remoteAddress = peer.localAddress();
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    public ChannelConfig config() {
        return this.config;
    }

    @Override
    public LocalServerChannel parent() {
        return (LocalServerChannel)super.parent();
    }

    @Override
    public LocalAddress localAddress() {
        return (LocalAddress)super.localAddress();
    }

    @Override
    public LocalAddress remoteAddress() {
        return (LocalAddress)super.remoteAddress();
    }

    @Override
    public boolean isOpen() {
        return this.state < 3;
    }

    @Override
    public boolean isActive() {
        return this.state == 2;
    }

    @Override
    protected Channel.Unsafe newUnsafe() {
        return new LocalUnsafe();
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return loop instanceof SingleThreadEventLoop;
    }

    @Override
    protected SocketAddress localAddress0() {
        return this.localAddress;
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return this.remoteAddress;
    }

    @Override
    protected Runnable doRegister() throws Exception {
        Runnable postRegisterTask;
        final LocalChannel peer = this.peer;
        if (peer != null) {
            this.state = 2;
            peer.remoteAddress = this.parent().localAddress();
            peer.state = 2;
            final EventLoop peerEventLoop = peer.eventLoop();
            postRegisterTask = new Runnable(){

                @Override
                public void run() {
                    peerEventLoop.execute(new Runnable(){

                        @Override
                        public void run() {
                            peer.connectFuture.setSuccess();
                            peer.pipeline().fireChannelActive();
                        }
                    });
                }

            };
        } else {
            postRegisterTask = null;
        }
        ((SingleThreadEventExecutor)((Object)this.eventLoop())).addShutdownHook(this.shutdownHook);
        return postRegisterTask;
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        this.localAddress = LocalChannelRegistry.register(this, this.localAddress, localAddress);
        this.state = 1;
    }

    @Override
    protected void doDisconnect() throws Exception {
        this.doClose();
    }

    @Override
    protected void doPreClose() throws Exception {
        if (this.state > 2) {
            return;
        }
        if (this.parent() == null) {
            LocalChannelRegistry.unregister(this.localAddress);
        }
        this.localAddress = null;
        this.state = 3;
    }

    @Override
    protected void doClose() throws Exception {
        if (this.peer.isActive()) {
            this.peer.unsafe().close(this.peer.unsafe().voidFuture());
            this.peer = null;
        }
    }

    @Override
    protected void doDeregister() throws Exception {
        if (this.isOpen()) {
            this.unsafe().close(this.unsafe().voidFuture());
        }
        ((SingleThreadEventExecutor)((Object)this.eventLoop())).removeShutdownHook(this.shutdownHook);
    }

    @Override
    protected void doFlushMessageBuffer(MessageBuf<Object> buf) throws Exception {
        if (this.state < 2) {
            throw new NotYetConnectedException();
        }
        if (this.state > 2) {
            throw new ClosedChannelException();
        }
        LocalChannel peer = this.peer;
        final ChannelPipeline peerPipeline = peer.pipeline();
        EventLoop peerLoop = peer.eventLoop();
        if (peerLoop == this.eventLoop()) {
            buf.drainTo(peerPipeline.inboundMessageBuffer());
            peerPipeline.fireInboundBufferUpdated();
        } else {
            final Object[] msgs = buf.toArray();
            buf.clear();
            peerLoop.execute(new Runnable(){

                @Override
                public void run() {
                    MessageBuf buf = peerPipeline.inboundMessageBuffer();
                    Collections.addAll(buf, msgs);
                    peerPipeline.fireInboundBufferUpdated();
                }
            });
        }
    }

    @Override
    protected boolean isFlushPending() {
        return false;
    }

    private class LocalUnsafe
    extends AbstractChannel.AbstractUnsafe {
        private LocalUnsafe() {
        }

        @Override
        public void connect(final SocketAddress remoteAddress, SocketAddress localAddress, final ChannelFuture future) {
            if (LocalChannel.this.eventLoop().inEventLoop()) {
                Channel boundChannel;
                if (!this.ensureOpen(future)) {
                    return;
                }
                if (LocalChannel.this.state == 2) {
                    AlreadyConnectedException cause = new AlreadyConnectedException();
                    future.setFailure(cause);
                    LocalChannel.this.pipeline().fireExceptionCaught(cause);
                    return;
                }
                if (LocalChannel.this.connectFuture != null) {
                    throw new ConnectionPendingException();
                }
                LocalChannel.this.connectFuture = future;
                if (LocalChannel.this.state != 1 && localAddress == null) {
                    localAddress = new LocalAddress(LocalChannel.this);
                }
                if (localAddress != null) {
                    try {
                        LocalChannel.this.doBind(localAddress);
                    }
                    catch (Throwable t) {
                        future.setFailure(t);
                        LocalChannel.this.pipeline().fireExceptionCaught(t);
                        this.close(this.voidFuture());
                        return;
                    }
                }
                if (!((boundChannel = LocalChannelRegistry.get(remoteAddress)) instanceof LocalServerChannel)) {
                    ChannelException cause = new ChannelException("connection refused");
                    future.setFailure(cause);
                    LocalChannel.this.pipeline().fireExceptionCaught(cause);
                    this.close(this.voidFuture());
                    return;
                }
                LocalServerChannel serverChannel = (LocalServerChannel)boundChannel;
                LocalChannel.this.peer = serverChannel.serve(LocalChannel.this);
            } else {
                final SocketAddress localAddress0 = localAddress;
                LocalChannel.this.eventLoop().execute(new Runnable(){

                    @Override
                    public void run() {
                        LocalUnsafe.this.connect(remoteAddress, localAddress0, future);
                    }
                });
            }
        }

        @Override
        public void suspendRead() {
        }

        @Override
        public void resumeRead() {
        }

    }

}

