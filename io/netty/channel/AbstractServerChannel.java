
package io.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ChannelBufType;
import io.netty.buffer.MessageBuf;
import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.NoSuchBufferException;
import io.netty.channel.ServerChannel;
import java.net.SocketAddress;

public abstract class AbstractServerChannel
extends AbstractChannel
implements ServerChannel {
    private static final ChannelMetadata METADATA = new ChannelMetadata(ChannelBufType.MESSAGE, false);

    protected AbstractServerChannel(Integer id) {
        super(null, id);
    }

    @Override
    public ByteBuf outboundByteBuffer() {
        throw new NoSuchBufferException(String.format("%s does not have an outbound buffer.", ServerChannel.class.getSimpleName()));
    }

    @Override
    public <T> MessageBuf<T> outboundMessageBuffer() {
        throw new NoSuchBufferException(String.format("%s does not have an outbound buffer.", ServerChannel.class.getSimpleName()));
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    public SocketAddress remoteAddress() {
        return null;
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
    protected void doFlushByteBuffer(ByteBuf buf) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doFlushMessageBuffer(MessageBuf<Object> buf) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean isFlushPending() {
        return false;
    }

    protected abstract class AbstractServerUnsafe
    extends AbstractChannel.AbstractUnsafe {
        protected AbstractServerUnsafe() {
        }

        @Override
        public void flush(final ChannelFuture future) {
            if (AbstractServerChannel.this.eventLoop().inEventLoop()) {
                this.reject(future);
            } else {
                AbstractServerChannel.this.eventLoop().execute(new Runnable(){

                    @Override
                    public void run() {
                        AbstractServerUnsafe.this.flush(future);
                    }
                });
            }
        }

        @Override
        public void connect(final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelFuture future) {
            if (AbstractServerChannel.this.eventLoop().inEventLoop()) {
                this.reject(future);
            } else {
                AbstractServerChannel.this.eventLoop().execute(new Runnable(){

                    @Override
                    public void run() {
                        AbstractServerUnsafe.this.connect(remoteAddress, localAddress, future);
                    }
                });
            }
        }

        private void reject(ChannelFuture future) {
            UnsupportedOperationException cause = new UnsupportedOperationException();
            future.setFailure(cause);
            AbstractServerChannel.this.pipeline().fireExceptionCaught(cause);
        }

    }

}

