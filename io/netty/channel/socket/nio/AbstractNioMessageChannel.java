
package io.netty.channel.socket.nio;

import io.netty.buffer.MessageBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.AbstractNioChannel;
import io.netty.channel.socket.nio.NioEventLoop;
import java.io.IOException;
import java.nio.channels.SelectableChannel;

abstract class AbstractNioMessageChannel
extends AbstractNioChannel {
    protected AbstractNioMessageChannel(Channel parent, Integer id, SelectableChannel ch, int readInterestOp) {
        super(parent, id, ch, readInterestOp);
    }

    @Override
    protected NioMessageUnsafe newUnsafe() {
        return new NioMessageUnsafe();
    }

    @Override
    protected void doFlushMessageBuffer(MessageBuf<Object> buf) throws Exception {
        int writeSpinCount = this.config().getWriteSpinCount() - 1;
        while (!buf.isEmpty()) {
            boolean wrote = false;
            for (int i = writeSpinCount; i >= 0; --i) {
                int localFlushedAmount = this.doWriteMessages(buf, i == 0);
                if (localFlushedAmount <= 0) continue;
                wrote = true;
                break;
            }
            if (wrote) continue;
            break;
        }
    }

    protected abstract int doReadMessages(MessageBuf<Object> var1) throws Exception;

    protected abstract int doWriteMessages(MessageBuf<Object> var1, boolean var2) throws Exception;

    private final class NioMessageUnsafe
    extends AbstractNioChannel.AbstractNioUnsafe {
        private NioMessageUnsafe() {
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         * Enabled force condition propagation
         * Lifted jumps to return sites
         */
        @Override
        public void read() {
            assert (AbstractNioMessageChannel.this.eventLoop().inEventLoop());
            ChannelPipeline pipeline = AbstractNioMessageChannel.this.pipeline();
            MessageBuf<Object> msgBuf = pipeline.inboundMessageBuffer();
            boolean closed = false;
            boolean read = false;
            try {
                do {
                    int localReadAmount;
                    if ((localReadAmount = AbstractNioMessageChannel.this.doReadMessages(msgBuf)) > 0) {
                        read = true;
                        continue;
                    }
                    if (localReadAmount == 0) return;
                    if (localReadAmount < 0) break;
                } while (true);
                closed = true;
                return;
            }
            catch (Throwable t) {
                if (read) {
                    read = false;
                    pipeline.fireInboundBufferUpdated();
                }
                AbstractNioMessageChannel.this.pipeline().fireExceptionCaught(t);
                if (!(t instanceof IOException)) return;
                this.close(this.voidFuture());
            }
            finally {
                if (read) {
                    pipeline.fireInboundBufferUpdated();
                }
                if (closed && AbstractNioMessageChannel.this.isOpen()) {
                    this.close(this.voidFuture());
                }
            }
        }
    }

}

