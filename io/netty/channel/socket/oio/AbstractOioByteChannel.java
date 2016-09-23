
package io.netty.channel.socket.oio;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInputShutdownEvent;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.oio.AbstractOioChannel;
import java.io.IOException;

abstract class AbstractOioByteChannel
extends AbstractOioChannel {
    private volatile boolean inputShutdown;

    protected AbstractOioByteChannel(Channel parent, Integer id) {
        super(parent, id);
    }

    boolean isInputShutdown() {
        return this.inputShutdown;
    }

    @Override
    protected OioByteUnsafe newUnsafe() {
        return new OioByteUnsafe();
    }

    @Override
    protected void doFlushByteBuffer(ByteBuf buf) throws Exception {
        while (buf.readable()) {
            this.doWriteBytes(buf);
        }
        buf.clear();
    }

    protected abstract int available();

    protected abstract int doReadBytes(ByteBuf var1) throws Exception;

    protected abstract void doWriteBytes(ByteBuf var1) throws Exception;

    private final class OioByteUnsafe
    extends AbstractOioChannel.AbstractOioUnsafe {
        private OioByteUnsafe() {
            super(AbstractOioByteChannel.this);
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void read() {
            assert (AbstractOioByteChannel.this.eventLoop().inEventLoop());
            if (AbstractOioByteChannel.this.inputShutdown) {
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {
                    // empty catch block
                }
                return;
            }
            ChannelPipeline pipeline = AbstractOioByteChannel.this.pipeline();
            ByteBuf byteBuf = pipeline.inboundByteBuffer();
            boolean closed = false;
            boolean read = false;
            try {
                do {
                    int localReadAmount;
                    if ((localReadAmount = AbstractOioByteChannel.this.doReadBytes(byteBuf)) > 0) {
                        read = true;
                    } else if (localReadAmount < 0) {
                        closed = true;
                    }
                    int available = AbstractOioByteChannel.this.available();
                    if (available > 0) {
                        int maxCapacity;
                        if (byteBuf.writable()) continue;
                        int capacity = byteBuf.capacity();
                        if (capacity == (maxCapacity = byteBuf.maxCapacity())) {
                            if (!read) continue;
                            read = false;
                            pipeline.fireInboundBufferUpdated();
                            if (byteBuf.writable()) continue;
                            throw new IllegalStateException("an inbound handler whose buffer is full must consume at least one byte.");
                        }
                        int writerIndex = byteBuf.writerIndex();
                        if (writerIndex + available > maxCapacity) {
                            byteBuf.capacity(maxCapacity);
                            continue;
                        }
                        byteBuf.ensureWritableBytes(available);
                        continue;
                    }
                    break;
                    break;
                } while (true);
            }
            catch (Throwable t) {
                if (read) {
                    read = false;
                    pipeline.fireInboundBufferUpdated();
                }
                AbstractOioByteChannel.this.pipeline().fireExceptionCaught(t);
                if (t instanceof IOException) {
                    this.close(this.voidFuture());
                }
            }
            finally {
                if (read) {
                    pipeline.fireInboundBufferUpdated();
                }
                if (closed) {
                    AbstractOioByteChannel.this.inputShutdown = true;
                    if (AbstractOioByteChannel.this.isOpen()) {
                        if (Boolean.TRUE.equals(AbstractOioByteChannel.this.config().getOption(ChannelOption.ALLOW_HALF_CLOSURE))) {
                            pipeline.fireUserEventTriggered(ChannelInputShutdownEvent.INSTANCE);
                        } else {
                            this.close(this.voidFuture());
                        }
                    }
                }
            }
        }
    }

}

