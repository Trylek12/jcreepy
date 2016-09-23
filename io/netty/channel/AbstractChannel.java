
package io.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.MessageBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFlushFutureNotifier;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultChannelFuture;
import io.netty.channel.DefaultChannelHandlerContext;
import io.netty.channel.DefaultChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.FailedChannelFuture;
import io.netty.channel.FileRegion;
import io.netty.channel.SucceededChannelFuture;
import io.netty.channel.VoidChannelFuture;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import io.netty.util.DefaultAttributeMap;
import io.netty.util.internal.DetectionUtil;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractChannel
extends DefaultAttributeMap
implements Channel {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractChannel.class);
    static final ConcurrentMap<Integer, Channel> allChannels = new ConcurrentHashMap<Integer, Channel>();
    private static final Random random = new Random();
    private final Channel parent;
    private final Integer id;
    private final Channel.Unsafe unsafe;
    private final DefaultChannelPipeline pipeline;
    private final ChannelFuture succeededFuture;
    private final ChannelFuture voidFuture;
    private final CloseFuture closeFuture;
    protected final ChannelFlushFutureNotifier flushFutureNotifier;
    private volatile SocketAddress localAddress;
    private volatile SocketAddress remoteAddress;
    private volatile EventLoop eventLoop;
    private volatile boolean registered;
    private ClosedChannelException closedChannelException;
    private boolean inFlushNow;
    private boolean flushNowPending;
    private boolean strValActive;
    private String strVal;
    private AbstractUnsafe.FlushTask flushTaskInProgress;

    private static Integer allocateId(Channel channel) {
        int idVal = random.nextInt();
        if (idVal > 0) {
            idVal = - idVal;
        } else if (idVal == 0) {
            idVal = -1;
        }
        Integer id;
        while (allChannels.putIfAbsent(id = Integer.valueOf(idVal), channel) != null) {
            if (--idVal < 0) continue;
            idVal = -1;
        }
        return id;
    }

    protected AbstractChannel(Channel parent, Integer id) {
        this.succeededFuture = new SucceededChannelFuture(this);
        this.voidFuture = new VoidChannelFuture(this);
        this.closeFuture = new CloseFuture(this);
        this.flushFutureNotifier = new ChannelFlushFutureNotifier();
        if (id == null) {
            id = AbstractChannel.allocateId(this);
        } else {
            if (id < 0) {
                throw new IllegalArgumentException("id: " + id + " (expected: >= 0)");
            }
            if (allChannels.putIfAbsent(id, this) != null) {
                throw new IllegalArgumentException("duplicate ID: " + id);
            }
        }
        this.parent = parent;
        this.id = id;
        this.unsafe = this.newUnsafe();
        this.pipeline = new DefaultChannelPipeline(this);
        this.closeFuture().addListener(new ChannelFutureListener(){

            @Override
            public void operationComplete(ChannelFuture future) {
                AbstractChannel.allChannels.remove(AbstractChannel.this.id());
            }
        });
    }

    @Override
    public final Integer id() {
        return this.id;
    }

    @Override
    public Channel parent() {
        return this.parent;
    }

    @Override
    public ChannelPipeline pipeline() {
        return this.pipeline;
    }

    @Override
    public ByteBufAllocator alloc() {
        return this.config().getAllocator();
    }

    @Override
    public EventLoop eventLoop() {
        EventLoop eventLoop = this.eventLoop;
        if (eventLoop == null) {
            throw new IllegalStateException("channel not registered to an event loop");
        }
        return eventLoop;
    }

    @Override
    public SocketAddress localAddress() {
        SocketAddress localAddress = this.localAddress;
        if (localAddress == null) {
            try {
                this.localAddress = localAddress = this.unsafe().localAddress();
            }
            catch (Throwable t) {
                return null;
            }
        }
        return localAddress;
    }

    protected void invalidateLocalAddress() {
        this.localAddress = null;
    }

    @Override
    public SocketAddress remoteAddress() {
        SocketAddress remoteAddress = this.remoteAddress;
        if (remoteAddress == null) {
            try {
                this.remoteAddress = remoteAddress = this.unsafe().remoteAddress();
            }
            catch (Throwable t) {
                return null;
            }
        }
        return remoteAddress;
    }

    protected void invalidateRemoteAddress() {
        this.remoteAddress = null;
    }

    @Override
    public boolean isRegistered() {
        return this.registered;
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        return this.pipeline.bind(localAddress);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress) {
        return this.pipeline.connect(remoteAddress);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
        return this.pipeline.connect(remoteAddress, localAddress);
    }

    @Override
    public ChannelFuture disconnect() {
        return this.pipeline.disconnect();
    }

    @Override
    public ChannelFuture close() {
        return this.pipeline.close();
    }

    @Override
    public ChannelFuture deregister() {
        return this.pipeline.deregister();
    }

    @Override
    public ChannelFuture flush() {
        return this.pipeline.flush();
    }

    @Override
    public ChannelFuture write(Object message) {
        return this.pipeline.write(message);
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress, ChannelFuture future) {
        return this.pipeline.bind(localAddress, future);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, ChannelFuture future) {
        return this.pipeline.connect(remoteAddress, future);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelFuture future) {
        return this.pipeline.connect(remoteAddress, localAddress, future);
    }

    @Override
    public ChannelFuture disconnect(ChannelFuture future) {
        return this.pipeline.disconnect(future);
    }

    @Override
    public ChannelFuture close(ChannelFuture future) {
        return this.pipeline.close(future);
    }

    @Override
    public ChannelFuture deregister(ChannelFuture future) {
        return this.pipeline.deregister(future);
    }

    @Override
    public ByteBuf outboundByteBuffer() {
        return this.pipeline.outboundByteBuffer();
    }

    @Override
    public <T> MessageBuf<T> outboundMessageBuffer() {
        return this.pipeline.outboundMessageBuffer();
    }

    @Override
    public ChannelFuture flush(ChannelFuture future) {
        return this.pipeline.flush(future);
    }

    @Override
    public ChannelFuture write(Object message, ChannelFuture future) {
        return this.pipeline.write(message, future);
    }

    @Override
    public ChannelFuture newFuture() {
        return new DefaultChannelFuture(this, false);
    }

    @Override
    public ChannelFuture newSucceededFuture() {
        return this.succeededFuture;
    }

    @Override
    public ChannelFuture newFailedFuture(Throwable cause) {
        return new FailedChannelFuture(this, cause);
    }

    @Override
    public ChannelFuture closeFuture() {
        return this.closeFuture;
    }

    @Override
    public Channel.Unsafe unsafe() {
        return this.unsafe;
    }

    @Override
    public ChannelFuture sendFile(FileRegion region) {
        return this.pipeline.sendFile(region);
    }

    @Override
    public ChannelFuture sendFile(FileRegion region, ChannelFuture future) {
        return this.pipeline.sendFile(region, future);
    }

    protected abstract Channel.Unsafe newUnsafe();

    public final int hashCode() {
        return this.id;
    }

    public final boolean equals(Object o) {
        return this == o;
    }

    @Override
    public final int compareTo(Channel o) {
        return this.id().compareTo(o.id());
    }

    public String toString() {
        boolean active = this.isActive();
        if (this.strValActive == active && this.strVal != null) {
            return this.strVal;
        }
        SocketAddress remoteAddr = this.remoteAddress();
        SocketAddress localAddr = this.localAddress();
        if (remoteAddr != null) {
            SocketAddress dstAddr;
            SocketAddress srcAddr;
            if (this.parent == null) {
                srcAddr = localAddr;
                dstAddr = remoteAddr;
            } else {
                srcAddr = remoteAddr;
                dstAddr = localAddr;
            }
            Object[] arrobject = new Object[4];
            arrobject[0] = this.id;
            arrobject[1] = srcAddr;
            arrobject[2] = active ? "=>" : ":>";
            arrobject[3] = dstAddr;
            this.strVal = String.format("[id: 0x%08x, %s %s %s]", arrobject);
        } else {
            this.strVal = localAddr != null ? String.format("[id: 0x%08x, %s]", this.id, localAddr) : String.format("[id: 0x%08x]", this.id);
        }
        this.strValActive = active;
        return this.strVal;
    }

    protected abstract boolean isCompatible(EventLoop var1);

    protected abstract SocketAddress localAddress0();

    protected abstract SocketAddress remoteAddress0();

    protected abstract Runnable doRegister() throws Exception;

    protected abstract void doBind(SocketAddress var1) throws Exception;

    protected abstract void doDisconnect() throws Exception;

    protected void doPreClose() throws Exception {
    }

    protected abstract void doClose() throws Exception;

    protected abstract void doDeregister() throws Exception;

    protected void doFlushByteBuffer(ByteBuf buf) throws Exception {
        throw new UnsupportedOperationException();
    }

    protected void doFlushMessageBuffer(MessageBuf<Object> buf) throws Exception {
        throw new UnsupportedOperationException();
    }

    protected void doFlushFileRegion(FileRegion region, ChannelFuture future) throws Exception {
        throw new UnsupportedOperationException();
    }

    protected static void checkEOF(FileRegion region, long writtenBytes) throws IOException {
        if (writtenBytes < region.count()) {
            throw new EOFException("Expected to be able to write " + region.count() + " bytes, but only wrote " + writtenBytes);
        }
    }

    protected abstract boolean isFlushPending();

    private final class CloseFuture
    extends DefaultChannelFuture
    implements ChannelFuture.Unsafe {
        CloseFuture(AbstractChannel ch) {
            super(ch, false);
        }

        @Override
        public boolean setSuccess() {
            throw new IllegalStateException();
        }

        @Override
        public boolean setFailure(Throwable cause) {
            throw new IllegalStateException();
        }

        boolean setClosed() {
            try {
                AbstractChannel.this.doPreClose();
            }
            catch (Exception e) {
                logger.warn("doPreClose() raised an exception.", e);
            }
            return super.setSuccess();
        }
    }

    private class FlushLater
    implements Runnable {
        private FlushLater() {
        }

        @Override
        public void run() {
            AbstractChannel.this.flushNowPending = false;
            AbstractChannel.this.unsafe().flush(AbstractChannel.this.voidFuture);
        }
    }

    protected abstract class AbstractUnsafe
    implements Channel.Unsafe {
        private final Runnable flushLaterTask;

        protected AbstractUnsafe() {
            this.flushLaterTask = new FlushLater();
        }

        @Override
        public final void sendFile(final FileRegion region, final ChannelFuture future) {
            if (AbstractChannel.this.eventLoop().inEventLoop()) {
                if (this.outboundBufSize() > 0) {
                    this.flushNotifier(AbstractChannel.this.newFuture()).addListener(new ChannelFutureListener(){

                        @Override
                        public void operationComplete(ChannelFuture cf) throws Exception {
                            AbstractUnsafe.this.sendFile0(region, future);
                        }
                    });
                } else {
                    this.sendFile0(region, future);
                }
            } else {
                AbstractChannel.this.eventLoop().execute(new Runnable(){

                    @Override
                    public void run() {
                        AbstractUnsafe.this.sendFile(region, future);
                    }
                });
            }
        }

        private void sendFile0(FileRegion region, ChannelFuture future) {
            FlushTask next;
            if (AbstractChannel.this.flushTaskInProgress == null) {
                AbstractChannel.this.flushTaskInProgress = new FlushTask(region, future);
                try {
                    AbstractChannel.this.doFlushFileRegion(region, future);
                }
                catch (Throwable cause) {
                    region.close();
                    future.setFailure(cause);
                }
                return;
            }
            FlushTask task = AbstractChannel.this.flushTaskInProgress;
            while ((next = task.next) != null) {
                task = next;
            }
            task.next = new FlushTask(region, future);
        }

        @Override
        public final ChannelHandlerContext directOutboundContext() {
            return AbstractChannel.access$400((AbstractChannel)AbstractChannel.this).head;
        }

        @Override
        public final ChannelFuture voidFuture() {
            return AbstractChannel.this.voidFuture;
        }

        @Override
        public final SocketAddress localAddress() {
            return AbstractChannel.this.localAddress0();
        }

        @Override
        public final SocketAddress remoteAddress() {
            return AbstractChannel.this.remoteAddress0();
        }

        @Override
        public final void register(EventLoop eventLoop, final ChannelFuture future) {
            if (eventLoop == null) {
                throw new NullPointerException("eventLoop");
            }
            if (AbstractChannel.this.isRegistered()) {
                throw new IllegalStateException("registered to an event loop already");
            }
            if (!AbstractChannel.this.isCompatible(eventLoop)) {
                throw new IllegalStateException("incompatible event loop type: " + eventLoop.getClass().getName());
            }
            AbstractChannel.this.eventLoop = eventLoop;
            assert (AbstractChannel.this.eventLoop().inEventLoop());
            if (!this.ensureOpen(future)) {
                return;
            }
            if (eventLoop.inEventLoop()) {
                this.register0(future);
            } else {
                eventLoop.execute(new Runnable(){

                    @Override
                    public void run() {
                        AbstractUnsafe.this.register0(future);
                    }
                });
            }
        }

        private void register0(ChannelFuture future) {
            try {
                Runnable postRegisterTask = AbstractChannel.this.doRegister();
                AbstractChannel.this.registered = true;
                future.setSuccess();
                AbstractChannel.this.pipeline.fireChannelRegistered();
                if (postRegisterTask != null) {
                    postRegisterTask.run();
                }
                if (AbstractChannel.this.isActive()) {
                    AbstractChannel.this.pipeline.fireChannelActive();
                }
            }
            catch (Throwable t) {
                try {
                    AbstractChannel.this.doClose();
                }
                catch (Throwable t2) {
                    logger.warn("Failed to close a channel", t2);
                }
                future.setFailure(t);
                AbstractChannel.this.closeFuture.setClosed();
            }
        }

        @Override
        public final void bind(final SocketAddress localAddress, final ChannelFuture future) {
            if (AbstractChannel.this.eventLoop().inEventLoop()) {
                if (!this.ensureOpen(future)) {
                    return;
                }
                try {
                    boolean wasActive = AbstractChannel.this.isActive();
                    if (!DetectionUtil.isWindows() && !DetectionUtil.isRoot() && Boolean.TRUE.equals(AbstractChannel.this.config().getOption(ChannelOption.SO_BROADCAST)) && localAddress instanceof InetSocketAddress && !((InetSocketAddress)localAddress).getAddress().isAnyLocalAddress()) {
                        logger.warn("A non-root user can't receive a broadcast packet if the socket is not bound to a wildcard address; binding to a non-wildcard address (" + localAddress + ") anyway as requested.");
                    }
                    AbstractChannel.this.doBind(localAddress);
                    future.setSuccess();
                    if (!wasActive && AbstractChannel.this.isActive()) {
                        AbstractChannel.this.pipeline.fireChannelActive();
                    }
                }
                catch (Throwable t) {
                    future.setFailure(t);
                    this.closeIfClosed();
                }
            } else {
                AbstractChannel.this.eventLoop().execute(new Runnable(){

                    @Override
                    public void run() {
                        AbstractUnsafe.this.bind(localAddress, future);
                    }
                });
            }
        }

        @Override
        public final void disconnect(final ChannelFuture future) {
            if (AbstractChannel.this.eventLoop().inEventLoop()) {
                try {
                    boolean wasActive = AbstractChannel.this.isActive();
                    AbstractChannel.this.doDisconnect();
                    future.setSuccess();
                    if (wasActive && !AbstractChannel.this.isActive()) {
                        AbstractChannel.this.pipeline.fireChannelInactive();
                    }
                }
                catch (Throwable t) {
                    future.setFailure(t);
                    this.closeIfClosed();
                }
            } else {
                AbstractChannel.this.eventLoop().execute(new Runnable(){

                    @Override
                    public void run() {
                        AbstractUnsafe.this.disconnect(future);
                    }
                });
            }
        }

        @Override
        public final void close(final ChannelFuture future) {
            if (AbstractChannel.this.eventLoop().inEventLoop()) {
                boolean wasActive = AbstractChannel.this.isActive();
                if (AbstractChannel.this.closeFuture.setClosed()) {
                    try {
                        AbstractChannel.this.doClose();
                        future.setSuccess();
                    }
                    catch (Throwable t) {
                        future.setFailure(t);
                    }
                    if (AbstractChannel.this.closedChannelException != null) {
                        AbstractChannel.this.closedChannelException = new ClosedChannelException();
                    }
                    AbstractChannel.this.flushFutureNotifier.notifyFlushFutures(AbstractChannel.this.closedChannelException);
                    if (wasActive && !AbstractChannel.this.isActive()) {
                        AbstractChannel.this.pipeline.fireChannelInactive();
                    }
                    this.deregister(this.voidFuture());
                } else {
                    future.setSuccess();
                }
            } else {
                AbstractChannel.this.eventLoop().execute(new Runnable(){

                    @Override
                    public void run() {
                        AbstractUnsafe.this.close(future);
                    }
                });
            }
        }

        @Override
        public final void closeForcibly() {
            try {
                AbstractChannel.this.doClose();
            }
            catch (Exception e) {
                logger.warn("Failed to close a channel.", e);
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public final void deregister(final ChannelFuture future) {
            if (AbstractChannel.this.eventLoop().inEventLoop()) {
                if (!AbstractChannel.this.registered) {
                    future.setSuccess();
                    return;
                }
                try {
                    AbstractChannel.this.doDeregister();
                }
                catch (Throwable t) {
                    logger.warn("Unexpected exception occurred while deregistering a channel.", t);
                }
                finally {
                    if (AbstractChannel.this.registered) {
                        AbstractChannel.this.registered = false;
                        future.setSuccess();
                        AbstractChannel.this.pipeline.fireChannelUnregistered();
                    } else {
                        future.setSuccess();
                    }
                }
            }
            AbstractChannel.this.eventLoop().execute(new Runnable(){

                @Override
                public void run() {
                    AbstractUnsafe.this.deregister(future);
                }
            });
        }

        @Override
        public void flush(final ChannelFuture future) {
            if (AbstractChannel.this.eventLoop().inEventLoop()) {
                if (AbstractChannel.this.flushTaskInProgress != null) {
                    FlushTask t;
                    FlushTask task = AbstractChannel.this.flushTaskInProgress;
                    while ((t = task.next) != null) {
                        task = t.next;
                    }
                    task.next = new FlushTask(null, future);
                    return;
                }
                this.flushNotifierAndFlush(future);
            } else {
                AbstractChannel.this.eventLoop().execute(new Runnable(){

                    @Override
                    public void run() {
                        AbstractUnsafe.this.flush(future);
                    }
                });
            }
        }

        private void flushNotifierAndFlush(ChannelFuture future) {
            this.flushNotifier(future);
            this.flush0();
        }

        private int outboundBufSize() {
            ChannelHandlerContext ctx = this.directOutboundContext();
            int bufSize = ctx.hasOutboundByteBuffer() ? ctx.outboundByteBuffer().readableBytes() : ctx.outboundMessageBuffer().size();
            return bufSize;
        }

        private ChannelFuture flushNotifier(ChannelFuture future) {
            if (future != AbstractChannel.this.voidFuture) {
                AbstractChannel.this.flushFutureNotifier.addFlushFuture(future, this.outboundBufSize());
            }
            return future;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        private void flush0() {
            block10 : {
                if (!AbstractChannel.this.inFlushNow) {
                    try {
                        if (!AbstractChannel.this.isFlushPending()) {
                            this.flushNow();
                        }
                        break block10;
                    }
                    catch (Throwable t) {
                        AbstractChannel.this.flushFutureNotifier.notifyFlushFutures(t);
                        if (t instanceof IOException) {
                            this.close(this.voidFuture());
                        }
                        break block10;
                    }
                    finally {
                        if (!AbstractChannel.this.isActive()) {
                            this.close(AbstractChannel.this.unsafe().voidFuture());
                        }
                    }
                }
                if (!AbstractChannel.this.flushNowPending) {
                    AbstractChannel.this.flushNowPending = true;
                    AbstractChannel.this.eventLoop().execute(this.flushLaterTask);
                }
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public final void flushNow() {
            if (AbstractChannel.this.inFlushNow || AbstractChannel.this.flushTaskInProgress != null) {
                return;
            }
            AbstractChannel.this.inFlushNow = true;
            ChannelHandlerContext ctx = this.directOutboundContext();
            Throwable cause = null;
            try {
                if (ctx.hasOutboundByteBuffer()) {
                    ByteBuf out = ctx.outboundByteBuffer();
                    int oldSize = out.readableBytes();
                    try {
                        AbstractChannel.this.doFlushByteBuffer(out);
                    }
                    catch (Throwable t) {
                        cause = t;
                    }
                    finally {
                        int newSize = out.readableBytes();
                        int writtenBytes = oldSize - newSize;
                        if (writtenBytes > 0) {
                            AbstractChannel.this.flushFutureNotifier.increaseWriteCounter(writtenBytes);
                            if (newSize == 0) {
                                out.discardReadBytes();
                            }
                        }
                    }
                }
                MessageBuf<Object> out = ctx.outboundMessageBuffer();
                int oldSize = out.size();
                try {
                    AbstractChannel.this.doFlushMessageBuffer(out);
                }
                catch (Throwable t) {
                    cause = t;
                }
                finally {
                    AbstractChannel.this.flushFutureNotifier.increaseWriteCounter(oldSize - out.size());
                }
                if (cause == null) {
                    AbstractChannel.this.flushFutureNotifier.notifyFlushFutures();
                } else {
                    AbstractChannel.this.flushFutureNotifier.notifyFlushFutures(cause);
                    if (cause instanceof IOException) {
                        this.close(this.voidFuture());
                    }
                }
            }
            finally {
                AbstractChannel.this.inFlushNow = false;
            }
        }

        protected final boolean ensureOpen(ChannelFuture future) {
            if (AbstractChannel.this.isOpen()) {
                return true;
            }
            ClosedChannelException e = new ClosedChannelException();
            future.setFailure(e);
            return false;
        }

        protected final void closeIfClosed() {
            if (AbstractChannel.this.isOpen()) {
                return;
            }
            this.close(this.voidFuture());
        }

        private final class FlushTask {
            final FileRegion region;
            final ChannelFuture future;
            FlushTask next;

            FlushTask(FileRegion region, ChannelFuture future) {
                this.region = region;
                this.future = future;
                future.addListener(new ChannelFutureListener(AbstractUnsafe.this){
                    final /* synthetic */ AbstractUnsafe val$this$1;

                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        block5 : {
                            AbstractChannel.this.flushTaskInProgress = FlushTask.this.next;
                            if (FlushTask.this.next != null) {
                                try {
                                    FileRegion region = FlushTask.this.next.region;
                                    if (region == null) {
                                        AbstractUnsafe.this.flushNotifierAndFlush(future);
                                        break block5;
                                    }
                                    AbstractChannel.this.doFlushFileRegion(region, future);
                                }
                                catch (Throwable cause) {
                                    future.setFailure(cause);
                                }
                            } else {
                                AbstractChannel.this.flushFutureNotifier.notifyFlushFutures();
                            }
                        }
                    }
                });
            }

        }

    }

}

