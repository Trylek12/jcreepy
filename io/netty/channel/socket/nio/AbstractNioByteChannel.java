
package io.netty.channel.socket.nio;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInputShutdownEvent;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.FileRegion;
import io.netty.channel.socket.nio.AbstractNioChannel;
import io.netty.channel.socket.nio.NioEventLoop;
import io.netty.channel.socket.nio.NioTask;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;

abstract class AbstractNioByteChannel
extends AbstractNioChannel {
    protected AbstractNioByteChannel(Channel parent, Integer id, SelectableChannel ch) {
        super(parent, id, ch, 1);
    }

    @Override
    protected AbstractNioChannel.AbstractNioUnsafe newUnsafe() {
        return new NioByteUnsafe();
    }

    @Override
    protected void doFlushByteBuffer(ByteBuf buf) throws Exception {
        int localFlushedAmount;
        if (!buf.readable()) {
            buf.clear();
            return;
        }
        for (int i = this.config().getWriteSpinCount() - 1; i >= 0 && (localFlushedAmount = this.doWriteBytes(buf, i == 0)) <= 0; --i) {
            if (buf.readable()) continue;
            buf.clear();
            break;
        }
    }

    @Override
    protected void doFlushFileRegion(FileRegion region, ChannelFuture future) throws Exception {
        if (!(this.javaChannel() instanceof WritableByteChannel)) {
            throw new UnsupportedOperationException("Underlying Channel is not of instance " + WritableByteChannel.class);
        }
        TransferTask transferTask = new TransferTask(region, (WritableByteChannel)((Object)this.javaChannel()), future);
        transferTask.transfer();
    }

    protected abstract int doReadBytes(ByteBuf var1) throws Exception;

    protected abstract int doWriteBytes(ByteBuf var1, boolean var2) throws Exception;

    private static int expandReadBuffer(ByteBuf byteBuf) {
        int writerIndex = byteBuf.writerIndex();
        int capacity = byteBuf.capacity();
        if (capacity != writerIndex) {
            return 0;
        }
        int maxCapacity = byteBuf.maxCapacity();
        if (capacity == maxCapacity) {
            if (byteBuf.readerIndex() != 0) {
                byteBuf.discardReadBytes();
                return 0;
            }
            return 2;
        }
        int increment = 4096;
        if (writerIndex + 4096 > maxCapacity) {
            byteBuf.capacity(maxCapacity);
        } else {
            byteBuf.ensureWritableBytes(4096);
        }
        return 1;
    }

    private final class TransferTask
    implements NioTask<SelectableChannel> {
        private long writtenBytes;
        private final FileRegion region;
        private final WritableByteChannel wch;
        private final ChannelFuture future;

        TransferTask(FileRegion region, WritableByteChannel wch, ChannelFuture future) {
            this.region = region;
            this.wch = wch;
            this.future = future;
        }

        public void transfer() {
            try {
                do {
                    long localWrittenBytes;
                    if ((localWrittenBytes = this.region.transferTo(this.wch, this.writtenBytes)) == 0) {
                        AbstractNioByteChannel.this.eventLoop().executeWhenWritable(AbstractNioByteChannel.this, this);
                        return;
                    }
                    if (localWrittenBytes == -1) {
                        AbstractNioByteChannel.checkEOF(this.region, this.writtenBytes);
                        this.future.setSuccess();
                        return;
                    }
                    this.writtenBytes += localWrittenBytes;
                } while (this.writtenBytes < this.region.count());
                this.region.close();
                this.future.setSuccess();
                return;
            }
            catch (Throwable cause) {
                this.region.close();
                this.future.setFailure(cause);
                return;
            }
        }

        @Override
        public void channelReady(SelectableChannel ch, SelectionKey key) throws Exception {
            this.transfer();
        }

        @Override
        public void channelUnregistered(SelectableChannel ch) throws Exception {
            if (this.writtenBytes < this.region.count()) {
                this.region.close();
                if (!AbstractNioByteChannel.this.isOpen()) {
                    this.future.setFailure(new ClosedChannelException());
                } else {
                    this.future.setFailure(new IllegalStateException("Channel was unregistered before the region could be fully written"));
                }
            }
        }
    }

    private final class NioByteUnsafe
    extends AbstractNioChannel.AbstractNioUnsafe {
        private NioByteUnsafe() {
            super(AbstractNioByteChannel.this);
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         * Enabled aggressive block sorting
         * Enabled unnecessary exception pruning
         * Enabled aggressive exception aggregation
         */
        @Override
        public void read() {
            assert (AbstractNioByteChannel.this.eventLoop().inEventLoop());
            ChannelPipeline pipeline = AbstractNioByteChannel.this.pipeline();
            ByteBuf byteBuf = pipeline.inboundByteBuffer();
            boolean closed = false;
            boolean read = false;
            try {
                AbstractNioByteChannel.expandReadBuffer(byteBuf);
                do {
                    int localReadAmount;
                    if ((localReadAmount = AbstractNioByteChannel.this.doReadBytes(byteBuf)) > 0) {
                        read = true;
                    } else if (localReadAmount < 0) {
                        closed = true;
                        return;
                    }
                    switch (AbstractNioByteChannel.expandReadBuffer(byteBuf)) {
                        case 0: {
                            return;
                        }
                        case 1: {
                            break;
                        }
                        case 2: {
                            if (!read) break;
                            read = false;
                            pipeline.fireInboundBufferUpdated();
                            if (byteBuf.writable()) break;
                            throw new IllegalStateException("an inbound handler whose buffer is full must consume at least one byte.");
                        }
                    }
                } while (true);
            }
            catch (Throwable t) {
                if (read) {
                    read = false;
                    pipeline.fireInboundBufferUpdated();
                }
                AbstractNioByteChannel.this.pipeline().fireExceptionCaught(t);
                if (!(t instanceof IOException)) return;
                this.close(this.voidFuture());
                return;
            }
            finally {
                if (read) {
                    pipeline.fireInboundBufferUpdated();
                }
                if (closed) {
                    AbstractNioByteChannel.this.setInputShutdown();
                    if (AbstractNioByteChannel.this.isOpen()) {
                        if (Boolean.TRUE.equals(AbstractNioByteChannel.this.config().getOption(ChannelOption.ALLOW_HALF_CLOSURE))) {
                            AbstractNioByteChannel.this.suspendReadTask.run();
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

