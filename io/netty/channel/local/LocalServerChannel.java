
package io.netty.channel.local;

import io.netty.buffer.MessageBuf;
import io.netty.channel.AbstractServerChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.EventLoop;
import io.netty.channel.SingleThreadEventExecutor;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalChannelRegistry;
import java.net.SocketAddress;

public class LocalServerChannel
extends AbstractServerChannel {
    private final ChannelConfig config = new DefaultChannelConfig();
    private final Runnable shutdownHook;
    private volatile int state;
    private volatile LocalAddress localAddress;

    public LocalServerChannel() {
        this(null);
    }

    public LocalServerChannel(Integer id) {
        super(id);
        this.shutdownHook = new Runnable(){

            @Override
            public void run() {
                LocalServerChannel.this.unsafe().close(LocalServerChannel.this.unsafe().voidFuture());
            }
        };
    }

    @Override
    public ChannelConfig config() {
        return this.config;
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
        return this.state < 2;
    }

    @Override
    public boolean isActive() {
        return this.state == 1;
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
    protected Runnable doRegister() throws Exception {
        ((SingleThreadEventExecutor)((Object)this.eventLoop())).addShutdownHook(this.shutdownHook);
        return null;
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        this.localAddress = LocalChannelRegistry.register(this, this.localAddress, localAddress);
        this.state = 1;
    }

    @Override
    protected void doPreClose() throws Exception {
        if (this.state > 1) {
            return;
        }
        LocalChannelRegistry.unregister(this.localAddress);
        this.localAddress = null;
        this.state = 2;
    }

    @Override
    protected void doClose() throws Exception {
    }

    @Override
    protected void doDeregister() throws Exception {
        ((SingleThreadEventExecutor)((Object)this.eventLoop())).removeShutdownHook(this.shutdownHook);
    }

    LocalChannel serve(LocalChannel peer) {
        LocalChannel child = new LocalChannel(this, peer);
        this.serve0(child);
        return child;
    }

    private void serve0(final LocalChannel child) {
        if (this.eventLoop().inEventLoop()) {
            this.pipeline().inboundMessageBuffer().add((LocalChannel)child);
            this.pipeline().fireInboundBufferUpdated();
        } else {
            this.eventLoop().execute(new Runnable(){

                @Override
                public void run() {
                    LocalServerChannel.this.serve0(child);
                }
            });
        }
    }

    @Override
    protected Channel.Unsafe newUnsafe() {
        return new LocalServerUnsafe();
    }

    private final class LocalServerUnsafe
    extends AbstractServerChannel.AbstractServerUnsafe {
        private LocalServerUnsafe() {
        }

        @Override
        public void suspendRead() {
        }

        @Override
        public void resumeRead() {
        }
    }

}

