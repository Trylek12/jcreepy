
package io.netty.channel.socket.oio;

import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.oio.OioEventLoop;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

abstract class AbstractOioChannel
extends AbstractChannel {
    static final int SO_TIMEOUT = 1000;
    protected volatile boolean readSuspended;

    protected AbstractOioChannel(Channel parent, Integer id) {
        super(parent, id);
    }

    @Override
    public InetSocketAddress localAddress() {
        return (InetSocketAddress)super.localAddress();
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return (InetSocketAddress)super.remoteAddress();
    }

    @Override
    public OioUnsafe unsafe() {
        return (OioUnsafe)super.unsafe();
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return loop instanceof OioEventLoop;
    }

    @Override
    protected Runnable doRegister() throws Exception {
        return null;
    }

    @Override
    protected void doDeregister() throws Exception {
    }

    @Override
    protected boolean isFlushPending() {
        return false;
    }

    protected abstract void doConnect(SocketAddress var1, SocketAddress var2) throws Exception;

    abstract class AbstractOioUnsafe
    extends AbstractChannel.AbstractUnsafe
    implements OioUnsafe {
        AbstractOioUnsafe() {
        }

        @Override
        public void connect(final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelFuture future) {
            if (AbstractOioChannel.this.eventLoop().inEventLoop()) {
                if (!this.ensureOpen(future)) {
                    return;
                }
                try {
                    boolean wasActive = AbstractOioChannel.this.isActive();
                    AbstractOioChannel.this.doConnect(remoteAddress, localAddress);
                    future.setSuccess();
                    if (!wasActive && AbstractOioChannel.this.isActive()) {
                        AbstractOioChannel.this.pipeline().fireChannelActive();
                    }
                }
                catch (Throwable t) {
                    future.setFailure(t);
                    this.closeIfClosed();
                }
            } else {
                AbstractOioChannel.this.eventLoop().execute(new Runnable(){

                    @Override
                    public void run() {
                        AbstractOioUnsafe.this.connect(remoteAddress, localAddress, future);
                    }
                });
            }
        }

        @Override
        public void suspendRead() {
            AbstractOioChannel.this.readSuspended = true;
        }

        @Override
        public void resumeRead() {
            AbstractOioChannel.this.readSuspended = false;
        }

    }

    public static interface OioUnsafe
    extends Channel.Unsafe {
        public void read();
    }

}

