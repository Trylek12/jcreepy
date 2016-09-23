
package io.netty.channel.socket.oio;

import io.netty.buffer.MessageBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.oio.AbstractOioChannel;
import java.io.IOException;

abstract class AbstractOioMessageChannel
extends AbstractOioChannel {
    protected AbstractOioMessageChannel(Channel parent, Integer id) {
        super(parent, id);
    }

    @Override
    protected OioMessageUnsafe newUnsafe() {
        return new OioMessageUnsafe();
    }

    @Override
    protected void doFlushMessageBuffer(MessageBuf<Object> buf) throws Exception {
        while (!buf.isEmpty()) {
            this.doWriteMessages(buf);
        }
    }

    protected abstract int doReadMessages(MessageBuf<Object> var1) throws Exception;

    protected abstract void doWriteMessages(MessageBuf<Object> var1) throws Exception;

    private final class OioMessageUnsafe
    extends AbstractOioChannel.AbstractOioUnsafe {
        private OioMessageUnsafe() {
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void read() {
            assert (AbstractOioMessageChannel.this.eventLoop().inEventLoop());
            ChannelPipeline pipeline = AbstractOioMessageChannel.this.pipeline();
            MessageBuf<Object> msgBuf = pipeline.inboundMessageBuffer();
            boolean closed = false;
            boolean read = false;
            try {
                int localReadAmount = AbstractOioMessageChannel.this.doReadMessages(msgBuf);
                if (localReadAmount > 0) {
                    read = true;
                } else if (localReadAmount < 0) {
                    closed = true;
                }
            }
            catch (Throwable t) {
                if (read) {
                    read = false;
                    pipeline.fireInboundBufferUpdated();
                }
                AbstractOioMessageChannel.this.pipeline().fireExceptionCaught(t);
                if (t instanceof IOException) {
                    this.close(this.voidFuture());
                }
            }
            finally {
                if (read) {
                    pipeline.fireInboundBufferUpdated();
                }
                if (closed && AbstractOioMessageChannel.this.isOpen()) {
                    this.close(this.voidFuture());
                }
            }
        }
    }

}

