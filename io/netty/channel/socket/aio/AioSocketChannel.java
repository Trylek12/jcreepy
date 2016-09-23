
package io.netty.channel.socket.aio;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ChannelBufType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFlushFutureNotifier;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInputShutdownEvent;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.FileRegion;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.SocketChannelConfig;
import io.netty.channel.socket.aio.AbstractAioChannel;
import io.netty.channel.socket.aio.AioCompletionHandler;
import io.netty.channel.socket.aio.AioEventLoopGroup;
import io.netty.channel.socket.aio.AioServerSocketChannel;
import io.netty.channel.socket.aio.AioSocketChannelConfig;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannel;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.InterruptedByTimeoutException;
import java.nio.channels.NetworkChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AioSocketChannel
extends AbstractAioChannel
implements SocketChannel {
    private static final ChannelMetadata METADATA = new ChannelMetadata(ChannelBufType.BYTE, false);
    private static final CompletionHandler<Void, AioSocketChannel> CONNECT_HANDLER = new ConnectHandler();
    private static final CompletionHandler<Integer, AioSocketChannel> WRITE_HANDLER = new WriteHandler<Integer>();
    private static final CompletionHandler<Integer, AioSocketChannel> READ_HANDLER = new ReadHandler<Integer>();
    private static final CompletionHandler<Long, AioSocketChannel> GATHERING_WRITE_HANDLER = new WriteHandler<Long>();
    private static final CompletionHandler<Long, AioSocketChannel> SCATTERING_READ_HANDLER = new ReadHandler<Long>();
    private final AioSocketChannelConfig config;
    private volatile boolean inputShutdown;
    private volatile boolean outputShutdown;
    private boolean asyncWriteInProgress;
    private boolean inDoFlushByteBuffer;
    private boolean asyncReadInProgress;
    private boolean inBeginRead;
    private final AtomicBoolean readSuspended = new AtomicBoolean();
    private final Runnable readTask;

    private static AsynchronousSocketChannel newSocket(AsynchronousChannelGroup group) {
        try {
            return AsynchronousSocketChannel.open(group);
        }
        catch (IOException e) {
            throw new ChannelException("Failed to open a socket.", e);
        }
    }

    public AioSocketChannel(AioEventLoopGroup eventLoop) {
        this(null, null, eventLoop, AioSocketChannel.newSocket(eventLoop.group));
    }

    AioSocketChannel(AioServerSocketChannel parent, Integer id, AioEventLoopGroup eventLoop, AsynchronousSocketChannel ch) {
        super(parent, id, eventLoop, ch);
        this.readTask = new Runnable(){

            @Override
            public void run() {
                AioSocketChannel.this.beginRead();
            }
        };
        this.config = new AioSocketChannelConfig(ch);
    }

    @Override
    public boolean isActive() {
        return this.javaChannel().isOpen() && this.remoteAddress0() != null;
    }

    @Override
    protected AsynchronousSocketChannel javaChannel() {
        return (AsynchronousSocketChannel)super.javaChannel();
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    public boolean isInputShutdown() {
        return this.inputShutdown;
    }

    @Override
    public boolean isOutputShutdown() {
        return this.outputShutdown;
    }

    @Override
    public ChannelFuture shutdownOutput() {
        final ChannelFuture future = this.newFuture();
        EventLoop loop = this.eventLoop();
        if (loop.inEventLoop()) {
            this.shutdownOutput(future);
        } else {
            loop.execute(new Runnable(){

                @Override
                public void run() {
                    AioSocketChannel.this.shutdownOutput(future);
                }
            });
        }
        return future;
    }

    private void shutdownOutput(ChannelFuture future) {
        try {
            this.javaChannel().shutdownOutput();
            this.outputShutdown = true;
            future.setSuccess();
        }
        catch (Throwable t) {
            future.setFailure(t);
        }
    }

    @Override
    protected void doConnect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelFuture future) {
        if (localAddress != null) {
            try {
                this.javaChannel().bind(localAddress);
            }
            catch (IOException e) {
                future.setFailure(e);
                return;
            }
        }
        this.javaChannel().connect(remoteAddress, this, CONNECT_HANDLER);
    }

    @Override
    protected InetSocketAddress localAddress0() {
        try {
            return (InetSocketAddress)this.javaChannel().getLocalAddress();
        }
        catch (IOException e) {
            return null;
        }
    }

    @Override
    protected InetSocketAddress remoteAddress0() {
        try {
            return (InetSocketAddress)this.javaChannel().getRemoteAddress();
        }
        catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Runnable doRegister() throws Exception {
        super.doRegister();
        if (this.remoteAddress() == null) {
            return null;
        }
        return new Runnable(){

            @Override
            public void run() {
                AioSocketChannel.this.beginRead();
            }
        };
    }

    private static void expandReadBuffer(ByteBuf byteBuf) {
        int writerIndex = byteBuf.writerIndex();
        int capacity = byteBuf.capacity();
        if (capacity != writerIndex) {
            return;
        }
        int maxCapacity = byteBuf.maxCapacity();
        if (capacity == maxCapacity) {
            return;
        }
        int increment = 4096;
        if (writerIndex + 4096 > maxCapacity) {
            byteBuf.capacity(maxCapacity);
        } else {
            byteBuf.ensureWritableBytes(4096);
        }
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        this.javaChannel().bind(localAddress);
    }

    @Override
    protected void doDisconnect() throws Exception {
        this.doClose();
    }

    @Override
    protected void doClose() throws Exception {
        this.javaChannel().close();
        this.inputShutdown = true;
        this.outputShutdown = true;
    }

    @Override
    protected boolean isFlushPending() {
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected void doFlushByteBuffer(ByteBuf buf) throws Exception {
        block12 : {
            if (this.inDoFlushByteBuffer || this.asyncWriteInProgress) {
                return;
            }
            this.inDoFlushByteBuffer = true;
            try {
                if (buf.readable()) {
                    while (!buf.unsafe().isFreed()) {
                        buf.discardReadBytes();
                        this.asyncWriteInProgress = true;
                        if (buf.hasNioBuffers()) {
                            ByteBuffer[] buffers = buf.nioBuffers(buf.readerIndex(), buf.readableBytes());
                            if (buffers.length == 1) {
                                this.javaChannel().write(buffers[0], this.config.getWriteTimeout(), TimeUnit.MILLISECONDS, this, WRITE_HANDLER);
                            } else {
                                this.javaChannel().write(buffers, 0, buffers.length, this.config.getWriteTimeout(), TimeUnit.MILLISECONDS, this, GATHERING_WRITE_HANDLER);
                            }
                        } else {
                            this.javaChannel().write(buf.nioBuffer(), this.config.getWriteTimeout(), TimeUnit.MILLISECONDS, this, WRITE_HANDLER);
                        }
                        if (this.asyncWriteInProgress) {
                            buf.unsafe().suspendIntermediaryDeallocations();
                        } else if (buf.readable()) continue;
                        break block12;
                    }
                    break block12;
                }
                this.flushFutureNotifier.notifyFlushFutures();
            }
            finally {
                this.inDoFlushByteBuffer = false;
            }
        }
    }

    @Override
    protected void doFlushFileRegion(FileRegion region, ChannelFuture future) throws Exception {
        region.transferTo(new WritableByteChannelAdapter(region, future), 0);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void beginRead() {
        if (this.inBeginRead || this.asyncReadInProgress || this.readSuspended.get()) {
            return;
        }
        this.inBeginRead = true;
        try {
            ByteBuf byteBuf;
            while (!(byteBuf = this.pipeline().inboundByteBuffer()).unsafe().isFreed()) {
                if (!byteBuf.readable()) {
                    byteBuf.discardReadBytes();
                }
                AioSocketChannel.expandReadBuffer(byteBuf);
                this.asyncReadInProgress = true;
                if (byteBuf.hasNioBuffers()) {
                    ByteBuffer[] buffers = byteBuf.nioBuffers(byteBuf.writerIndex(), byteBuf.writableBytes());
                    if (buffers.length == 1) {
                        this.javaChannel().read(buffers[0], this.config.getReadTimeout(), TimeUnit.MILLISECONDS, this, READ_HANDLER);
                    } else {
                        this.javaChannel().read(buffers, 0, buffers.length, this.config.getReadTimeout(), TimeUnit.MILLISECONDS, this, SCATTERING_READ_HANDLER);
                    }
                } else {
                    ByteBuffer buffer = byteBuf.nioBuffer(byteBuf.writerIndex(), byteBuf.writableBytes());
                    this.javaChannel().read(buffer, this.config.getReadTimeout(), TimeUnit.MILLISECONDS, this, READ_HANDLER);
                }
                if (!this.asyncReadInProgress) continue;
                break;
            }
        }
        finally {
            this.inBeginRead = false;
        }
    }

    @Override
    public SocketChannelConfig config() {
        return this.config;
    }

    @Override
    protected Channel.Unsafe newUnsafe() {
        return new AioSocketChannelAsyncUnsafe();
    }

    private final class WritableByteChannelAdapter
    implements WritableByteChannel {
        private final FileRegion region;
        private final ChannelFuture future;
        private long written;

        public WritableByteChannelAdapter(FileRegion region, ChannelFuture future) {
            this.region = region;
            this.future = future;
        }

        @Override
        public int write(final ByteBuffer src) {
            AioSocketChannel.this.javaChannel().write(src, null, new CompletionHandler<Integer, Object>(){

                @Override
                public void completed(Integer result, Object attachment) {
                    try {
                        if (result == 0) {
                            AioSocketChannel.this.javaChannel().write(src, null, this);
                            return;
                        }
                        if (result == -1) {
                            AioSocketChannel.checkEOF(WritableByteChannelAdapter.this.region, WritableByteChannelAdapter.this.written);
                            WritableByteChannelAdapter.this.future.setSuccess();
                            return;
                        }
                        WritableByteChannelAdapter.access$1514(WritableByteChannelAdapter.this, result.intValue());
                        if (WritableByteChannelAdapter.this.written >= WritableByteChannelAdapter.this.region.count()) {
                            WritableByteChannelAdapter.this.region.close();
                            WritableByteChannelAdapter.this.future.setSuccess();
                            return;
                        }
                        if (src.hasRemaining()) {
                            AioSocketChannel.this.javaChannel().write(src, null, this);
                        } else {
                            WritableByteChannelAdapter.this.region.transferTo(WritableByteChannelAdapter.this, WritableByteChannelAdapter.this.written);
                        }
                    }
                    catch (Throwable cause) {
                        WritableByteChannelAdapter.this.region.close();
                        WritableByteChannelAdapter.this.future.setFailure(cause);
                    }
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    WritableByteChannelAdapter.this.region.close();
                    WritableByteChannelAdapter.this.future.setFailure(exc);
                }
            });
            return 0;
        }

        @Override
        public boolean isOpen() {
            return AioSocketChannel.this.javaChannel().isOpen();
        }

        @Override
        public void close() throws IOException {
            AioSocketChannel.this.javaChannel().close();
        }

        static /* synthetic */ long access$1514(WritableByteChannelAdapter x0, long x1) {
            return x0.written += x1;
        }

    }

    private final class AioSocketChannelAsyncUnsafe
    extends AbstractAioChannel.AbstractAioUnsafe {
        private AioSocketChannelAsyncUnsafe() {
        }

        @Override
        public void suspendRead() {
            AioSocketChannel.this.readSuspended.set(true);
        }

        @Override
        public void resumeRead() {
            if (AioSocketChannel.this.readSuspended.compareAndSet(true, false)) {
                if (AioSocketChannel.this.inputShutdown) {
                    return;
                }
                if (AioSocketChannel.this.eventLoop().inEventLoop()) {
                    AioSocketChannel.this.beginRead();
                } else {
                    AioSocketChannel.this.eventLoop().execute(AioSocketChannel.this.readTask);
                }
            }
        }
    }

    private static final class ConnectHandler
    extends AioCompletionHandler<Void, AioSocketChannel> {
        private ConnectHandler() {
        }

        @Override
        protected void completed0(Void result, AioSocketChannel channel) {
            ((AbstractAioChannel.AbstractAioUnsafe)channel.unsafe()).connectSuccess();
            channel.pipeline().fireChannelActive();
            channel.beginRead();
        }

        @Override
        protected void failed0(Throwable exc, AioSocketChannel channel) {
            ((AbstractAioChannel.AbstractAioUnsafe)channel.unsafe()).connectFailed(exc);
        }
    }

    private static final class ReadHandler<T extends Number>
    extends AioCompletionHandler<T, AioSocketChannel> {
        private ReadHandler() {
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        protected void completed0(T result, AioSocketChannel channel) {
            channel.asyncReadInProgress = false;
            if (channel.inputShutdown) {
                return;
            }
            ChannelPipeline pipeline = channel.pipeline();
            ByteBuf byteBuf = pipeline.inboundByteBuffer();
            boolean closed = false;
            boolean read = false;
            try {
                int localReadAmount = result.intValue();
                if (localReadAmount > 0) {
                    byteBuf.writerIndex(byteBuf.writerIndex() + localReadAmount);
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
                if (!(t instanceof ClosedChannelException)) {
                    pipeline.fireExceptionCaught(t);
                    if (t instanceof IOException) {
                        channel.unsafe().close(channel.unsafe().voidFuture());
                    }
                }
            }
            finally {
                if (read) {
                    pipeline.fireInboundBufferUpdated();
                }
                if (closed || !channel.isOpen()) {
                    channel.inputShutdown = true;
                    if (channel.isOpen()) {
                        if (channel.config().isAllowHalfClosure()) {
                            pipeline.fireUserEventTriggered(ChannelInputShutdownEvent.INSTANCE);
                        } else {
                            channel.unsafe().close(channel.unsafe().voidFuture());
                        }
                    }
                } else {
                    channel.beginRead();
                }
            }
        }

        @Override
        protected void failed0(Throwable t, AioSocketChannel channel) {
            channel.asyncReadInProgress = false;
            if (t instanceof ClosedChannelException) {
                return;
            }
            channel.pipeline().fireExceptionCaught(t);
            if (t instanceof IOException || t instanceof InterruptedByTimeoutException) {
                channel.unsafe().close(channel.unsafe().voidFuture());
            } else {
                channel.beginRead();
            }
        }
    }

    private static final class WriteHandler<T extends Number>
    extends AioCompletionHandler<T, AioSocketChannel> {
        private WriteHandler() {
        }

        @Override
        protected void completed0(T result, AioSocketChannel channel) {
            channel.asyncWriteInProgress = false;
            ByteBuf buf = channel.unsafe().directOutboundContext().outboundByteBuffer();
            buf.unsafe().resumeIntermediaryDeallocations();
            int writtenBytes = result.intValue();
            if (writtenBytes > 0) {
                buf.readerIndex(buf.readerIndex() + writtenBytes);
            }
            if (channel.inDoFlushByteBuffer) {
                return;
            }
            ChannelFlushFutureNotifier notifier = channel.flushFutureNotifier;
            notifier.increaseWriteCounter(writtenBytes);
            notifier.notifyFlushFutures();
            if (!channel.isActive()) {
                return;
            }
            if (buf.readable()) {
                channel.unsafe().flushNow();
            } else {
                buf.discardReadBytes();
            }
        }

        @Override
        protected void failed0(Throwable cause, AioSocketChannel channel) {
            ByteBuf buf;
            channel.asyncWriteInProgress = false;
            channel.flushFutureNotifier.notifyFlushFutures(cause);
            if (cause instanceof InterruptedByTimeoutException) {
                channel.unsafe().close(channel.unsafe().voidFuture());
                return;
            }
            if (!channel.inDoFlushByteBuffer && !(buf = channel.unsafe().directOutboundContext().outboundByteBuffer()).readable()) {
                buf.discardReadBytes();
            }
        }
    }

}

