
package io.netty.handler.ssl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.ChannelBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFlushFutureNotifier;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundByteHandler;
import io.netty.channel.ChannelOutboundByteHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultChannelFuture;
import io.netty.channel.EventExecutor;
import io.netty.channel.EventLoop;
import io.netty.handler.ssl.ImmediateExecutor;
import io.netty.handler.ssl.NotSslRecordException;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import io.netty.util.internal.DetectionUtil;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

public class SslHandler
extends ChannelHandlerAdapter
implements ChannelInboundByteHandler,
ChannelOutboundByteHandler {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(SslHandler.class);
    private static final Pattern IGNORABLE_CLASS_IN_STACK = Pattern.compile("^.*(Socket|DatagramChannel|SctpChannel).*$");
    private static final Pattern IGNORABLE_ERROR_MESSAGE = Pattern.compile("^.*(?:connection.*reset|connection.*closed|broken.*pipe).*$", 2);
    private volatile ChannelHandlerContext ctx;
    private final SSLEngine engine;
    private final Executor delegatedTaskExecutor;
    private final ChannelFlushFutureNotifier flushFutureNotifier = new ChannelFlushFutureNotifier();
    private final boolean startTls;
    private boolean sentFirstMessage;
    private final Queue<ChannelFuture> handshakeFutures = new ArrayDeque<ChannelFuture>();
    private final SSLEngineInboundCloseFuture sslCloseFuture;
    private volatile long handshakeTimeoutMillis;
    private volatile long closeNotifyTimeoutMillis;

    public SslHandler(SSLEngine engine) {
        this(engine, ImmediateExecutor.INSTANCE);
    }

    public SslHandler(SSLEngine engine, boolean startTls) {
        this(engine, startTls, ImmediateExecutor.INSTANCE);
    }

    public SslHandler(SSLEngine engine, Executor delegatedTaskExecutor) {
        this(engine, false, delegatedTaskExecutor);
    }

    public SslHandler(SSLEngine engine, boolean startTls, Executor delegatedTaskExecutor) {
        this.sslCloseFuture = new SSLEngineInboundCloseFuture();
        this.handshakeTimeoutMillis = 10000;
        this.closeNotifyTimeoutMillis = 3000;
        if (engine == null) {
            throw new NullPointerException("engine");
        }
        if (delegatedTaskExecutor == null) {
            throw new NullPointerException("delegatedTaskExecutor");
        }
        this.engine = engine;
        this.delegatedTaskExecutor = delegatedTaskExecutor;
        this.startTls = startTls;
    }

    public long getHandshakeTimeoutMillis() {
        return this.handshakeTimeoutMillis;
    }

    public void setHandshakeTimeout(long handshakeTimeout, TimeUnit unit) {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        this.setHandshakeTimeoutMillis(unit.toMillis(handshakeTimeout));
    }

    public void setHandshakeTimeoutMillis(long handshakeTimeoutMillis) {
        if (handshakeTimeoutMillis < 0) {
            throw new IllegalArgumentException("handshakeTimeoutMillis: " + handshakeTimeoutMillis + " (expected: >= 0)");
        }
        this.handshakeTimeoutMillis = handshakeTimeoutMillis;
    }

    public long getCloseNotifyTimeoutMillis() {
        return this.handshakeTimeoutMillis;
    }

    public void setCloseNotifyTimeout(long closeNotifyTimeout, TimeUnit unit) {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        this.setCloseNotifyTimeoutMillis(unit.toMillis(closeNotifyTimeout));
    }

    public void setCloseNotifyTimeoutMillis(long closeNotifyTimeoutMillis) {
        if (closeNotifyTimeoutMillis < 0) {
            throw new IllegalArgumentException("closeNotifyTimeoutMillis: " + closeNotifyTimeoutMillis + " (expected: >= 0)");
        }
        this.closeNotifyTimeoutMillis = closeNotifyTimeoutMillis;
    }

    public SSLEngine getEngine() {
        return this.engine;
    }

    public ChannelFuture handshake() {
        return this.handshake(this.ctx.newFuture());
    }

    public ChannelFuture handshake(final ChannelFuture future) {
        final ChannelHandlerContext ctx = this.ctx;
        final ScheduledFuture timeoutFuture = this.handshakeTimeoutMillis > 0 ? ctx.executor().schedule(new Runnable(){

            @Override
            public void run() {
                if (future.isDone()) {
                    return;
                }
                SSLException e = new SSLException("handshake timed out");
                if (future.setFailure(e)) {
                    ctx.fireExceptionCaught(e);
                    ctx.close();
                }
            }
        }, this.handshakeTimeoutMillis, TimeUnit.MILLISECONDS) : null;
        ctx.executor().execute(new Runnable(){

            @Override
            public void run() {
                block3 : {
                    try {
                        if (timeoutFuture != null) {
                            timeoutFuture.cancel(false);
                        }
                        SslHandler.this.engine.beginHandshake();
                        SslHandler.this.handshakeFutures.add(future);
                        SslHandler.this.flush(ctx, ctx.newFuture());
                    }
                    catch (Exception e) {
                        if (!future.setFailure(e)) break block3;
                        ctx.fireExceptionCaught(e);
                        ctx.close();
                    }
                }
            }
        });
        return future;
    }

    public ChannelFuture close() {
        return this.close(this.ctx.newFuture());
    }

    public ChannelFuture close(final ChannelFuture future) {
        final ChannelHandlerContext ctx = this.ctx;
        ctx.executor().execute(new Runnable(){

            @Override
            public void run() {
                SslHandler.this.engine.closeOutbound();
                ctx.flush(future);
            }
        });
        return future;
    }

    public ChannelFuture sslCloseFuture() {
        return this.sslCloseFuture;
    }

    @Override
    public ByteBuf newOutboundBuffer(ChannelHandlerContext ctx) throws Exception {
        return ctx.alloc().buffer();
    }

    @Override
    public ByteBuf newInboundBuffer(ChannelHandlerContext ctx) throws Exception {
        return ctx.alloc().buffer();
    }

    @Override
    public void freeInboundBuffer(ChannelHandlerContext ctx, ChannelBuf buf) throws Exception {
        buf.unsafe().free();
    }

    @Override
    public void freeOutboundBuffer(ChannelHandlerContext ctx, ChannelBuf buf) throws Exception {
        buf.unsafe().free();
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
        this.closeOutboundAndChannel(ctx, future, true);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
        this.closeOutboundAndChannel(ctx, future, false);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void flush(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
        ByteBuf in = ctx.outboundByteBuffer();
        ByteBuf out = ctx.nextOutboundByteBuffer();
        out.unsafe().discardSomeReadBytes();
        if (this.startTls && !this.sentFirstMessage) {
            this.sentFirstMessage = true;
            out.writeBytes(in);
            ctx.flush(future);
            return;
        }
        if (ctx.executor() == ctx.channel().eventLoop()) {
            this.flushFutureNotifier.addFlushFuture(future, in.readableBytes());
        } else {
            ChannelFlushFutureNotifier channelFlushFutureNotifier = this.flushFutureNotifier;
            synchronized (channelFlushFutureNotifier) {
                this.flushFutureNotifier.addFlushFuture(future, in.readableBytes());
            }
        }
        boolean unwrapLater = false;
        int bytesConsumed = 0;
        try {
            block15 : do {
                SSLEngineResult result = SslHandler.wrap(this.engine, in, out);
                bytesConsumed += result.bytesConsumed();
                if (result.getStatus() == SSLEngineResult.Status.CLOSED) {
                    if (in.readable()) {
                        in.clear();
                        SSLException e = new SSLException("SSLEngine already closed");
                        future.setFailure(e);
                        ctx.fireExceptionCaught(e);
                        this.flush0(ctx, bytesConsumed, e);
                        bytesConsumed = 0;
                    }
                    break;
                }
                switch (result.getHandshakeStatus()) {
                    case NEED_WRAP: {
                        ctx.flush();
                        continue block15;
                    }
                    case NEED_UNWRAP: {
                        if (!ctx.inboundByteBuffer().readable()) break;
                        unwrapLater = true;
                        break;
                    }
                    case NEED_TASK: {
                        this.runDelegatedTasks();
                        continue block15;
                    }
                    case FINISHED: {
                        this.setHandshakeSuccess();
                        continue block15;
                    }
                    case NOT_HANDSHAKING: {
                        break;
                    }
                    default: {
                        throw new IllegalStateException("Unknown handshake status: " + (Object)((Object)result.getHandshakeStatus()));
                    }
                }
                if (result.bytesConsumed() == 0 && result.bytesProduced() == 0) break;
            } while (true);
            if (unwrapLater) {
                this.inboundBufferUpdated(ctx);
            }
        }
        catch (SSLException e) {
            this.setHandshakeFailure(e);
            throw e;
        }
        finally {
            in.unsafe().discardSomeReadBytes();
            this.flush0(ctx, bytesConsumed);
        }
    }

    private void flush0(final ChannelHandlerContext ctx, final int bytesConsumed) {
        ctx.flush(ctx.newFuture().addListener(new ChannelFutureListener(){

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (ctx.executor() == ctx.channel().eventLoop()) {
                    this.notifyFlushFutures(bytesConsumed, future);
                } else {
                    ChannelFlushFutureNotifier channelFlushFutureNotifier = SslHandler.this.flushFutureNotifier;
                    synchronized (channelFlushFutureNotifier) {
                        this.notifyFlushFutures(bytesConsumed, future);
                    }
                }
            }

            private void notifyFlushFutures(int bytesConsumed2, ChannelFuture future) {
                if (future.isSuccess()) {
                    SslHandler.this.flushFutureNotifier.increaseWriteCounter(bytesConsumed2);
                    SslHandler.this.flushFutureNotifier.notifyFlushFutures();
                } else {
                    SslHandler.this.flushFutureNotifier.notifyFlushFutures(future.cause());
                }
            }
        }));
    }

    private void flush0(final ChannelHandlerContext ctx, final int bytesConsumed, final Throwable cause) {
        ChannelFuture flushFuture = ctx.flush(ctx.newFuture().addListener(new ChannelFutureListener(){

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (ctx.executor() == ctx.channel().eventLoop()) {
                    this.notifyFlushFutures(bytesConsumed, cause, future);
                } else {
                    ChannelFlushFutureNotifier channelFlushFutureNotifier = SslHandler.this.flushFutureNotifier;
                    synchronized (channelFlushFutureNotifier) {
                        this.notifyFlushFutures(bytesConsumed, cause, future);
                    }
                }
            }

            private void notifyFlushFutures(int bytesConsumed2, Throwable cause2, ChannelFuture future) {
                SslHandler.this.flushFutureNotifier.increaseWriteCounter(bytesConsumed2);
                if (future.isSuccess()) {
                    SslHandler.this.flushFutureNotifier.notifyFlushFutures(cause2);
                } else {
                    SslHandler.this.flushFutureNotifier.notifyFlushFutures(cause2, future.cause());
                }
            }
        }));
        this.safeClose(ctx, flushFuture, ctx.newFuture());
    }

    private static SSLEngineResult wrap(SSLEngine engine, ByteBuf in, ByteBuf out) throws SSLException {
        SSLEngineResult result;
        ByteBuffer in0 = in.nioBuffer();
        do {
            ByteBuffer out0 = out.nioBuffer(out.writerIndex(), out.writableBytes());
            result = engine.wrap(in0, out0);
            in.skipBytes(result.bytesConsumed());
            out.writerIndex(out.writerIndex() + result.bytesProduced());
            if (result.getStatus() != SSLEngineResult.Status.BUFFER_OVERFLOW) break;
            out.ensureWritableBytes(engine.getSession().getPacketBufferSize());
        } while (true);
        return result;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.setHandshakeFailure(null);
        try {
            this.inboundBufferUpdated(ctx);
        }
        finally {
            block7 : {
                this.engine.closeOutbound();
                try {
                    this.engine.closeInbound();
                }
                catch (SSLException ex) {
                    if (!logger.isDebugEnabled()) break block7;
                    logger.debug("Failed to clean up SSLEngine.", ex);
                }
            }
            ctx.fireChannelInactive();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (this.ignoreException(cause)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Swallowing a 'connection reset by peer / broken pipe' error occurred while writing 'closure_notify'", cause);
            }
            if (ctx.channel().isActive()) {
                ctx.close();
            }
            return;
        }
        super.exceptionCaught(ctx, cause);
    }

    private boolean ignoreException(Throwable t) {
        if (!(t instanceof SSLException) && t instanceof IOException && this.engine.isOutboundDone()) {
            StackTraceElement[] elements;
            String message = String.valueOf(t.getMessage()).toLowerCase();
            if (IGNORABLE_ERROR_MESSAGE.matcher(message).matches()) {
                return true;
            }
            for (StackTraceElement element : elements = t.getStackTrace()) {
                String classname = element.getClassName();
                String methodname = element.getMethodName();
                if (classname.startsWith("io.netty.") || !"read".equals(methodname)) continue;
                if (IGNORABLE_CLASS_IN_STACK.matcher(classname).matches()) {
                    return true;
                }
                try {
                    Class clazz = this.getClass().getClassLoader().loadClass(classname);
                    if (SocketChannel.class.isAssignableFrom(clazz) || DatagramChannel.class.isAssignableFrom(clazz)) {
                        return true;
                    }
                    if (DetectionUtil.javaVersion() < 7 || !"com.sun.nio.sctp.SctpChannel".equals(clazz.getSuperclass().getName())) continue;
                    return true;
                }
                catch (ClassNotFoundException e) {
                    // empty catch block
                }
            }
        }
        return false;
    }

    public static boolean isEncrypted(ByteBuf buffer) {
        return SslHandler.getEncryptedPacketLength(buffer) != -1;
    }

    private static int getEncryptedPacketLength(ByteBuf buffer) {
        boolean tls;
        if (buffer.readableBytes() < 5) {
            throw new IllegalArgumentException("buffer must have at least 5 readable bytes");
        }
        int packetLength = 0;
        switch (buffer.getUnsignedByte(buffer.readerIndex())) {
            case 20: 
            case 21: 
            case 22: 
            case 23: {
                tls = true;
                break;
            }
            default: {
                tls = false;
            }
        }
        if (tls) {
            short majorVersion = buffer.getUnsignedByte(buffer.readerIndex() + 1);
            if (majorVersion == 3) {
                packetLength = (SslHandler.getShort(buffer, buffer.readerIndex() + 3) & 65535) + 5;
                if (packetLength <= 5) {
                    tls = false;
                }
            } else {
                tls = false;
            }
        }
        if (!tls) {
            boolean sslv2 = true;
            int headerLength = (buffer.getUnsignedByte(buffer.readerIndex()) & 128) != 0 ? 2 : 3;
            short majorVersion = buffer.getUnsignedByte(buffer.readerIndex() + headerLength + 1);
            if (majorVersion == 2 || majorVersion == 3) {
                packetLength = headerLength == 2 ? (SslHandler.getShort(buffer, buffer.readerIndex()) & 32767) + 2 : (SslHandler.getShort(buffer, buffer.readerIndex()) & 16383) + 3;
                if (packetLength <= headerLength) {
                    sslv2 = false;
                }
            } else {
                sslv2 = false;
            }
            if (!sslv2) {
                return -1;
            }
        }
        return packetLength;
    }

    /*
     * Unable to fully structure code
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     * Lifted jumps to return sites
     */
    @Override
    public void inboundBufferUpdated(ChannelHandlerContext ctx) throws Exception {
        in = ctx.inboundByteBuffer();
        if (in.readableBytes() < 5) {
            return;
        }
        packetLength = SslHandler.getEncryptedPacketLength(in);
        if (packetLength == -1) {
            e = new NotSslRecordException("not an SSL/TLS record: " + ByteBufUtil.hexDump(in));
            in.skipBytes(in.readableBytes());
            ctx.fireExceptionCaught(e);
            this.setHandshakeFailure(e);
            return;
        }
        if (!SslHandler.$assertionsDisabled && packetLength <= 0) {
            throw new AssertionError();
        }
        out = ctx.nextInboundByteBuffer();
        out.discardReadBytes();
        wrapLater = false;
        bytesProduced = 0;
        try {
            block16 : do {
                result = SslHandler.unwrap(this.engine, in, out);
                bytesProduced += result.bytesProduced();
                switch (9.$SwitchMap$javax$net$ssl$SSLEngineResult$Status[result.getStatus().ordinal()]) {
                    case 1: {
                        this.sslCloseFuture.setClosed();
                        break;
                    }
                    case 2: {
                        ** break;
                    }
                }
                switch (9.$SwitchMap$javax$net$ssl$SSLEngineResult$HandshakeStatus[result.getHandshakeStatus().ordinal()]) {
                    case 2: {
                        ** break;
                    }
                    case 1: {
                        wrapLater = true;
                        ** break;
                    }
                    case 3: {
                        this.runDelegatedTasks();
                        ** break;
                    }
                    case 4: {
                        this.setHandshakeSuccess();
                        wrapLater = true;
                        continue block16;
                    }
                    case 5: {
                        ** break;
                    }
                }
                throw new IllegalStateException("Unknown handshake status: " + (Object)result.getHandshakeStatus());
lbl43: // 4 sources:
                if (result.bytesConsumed() == 0 && result.bytesProduced() == 0) break;
            } while (true);
            ** break;
lbl46: // 2 sources:
            if (wrapLater == false) return;
            this.flush(ctx, ctx.newFuture());
            return;
        }
        catch (SSLException e) {
            this.setHandshakeFailure(e);
            throw e;
        }
        finally {
            if (bytesProduced > 0) {
                in.discardReadBytes();
                ctx.fireInboundBufferUpdated();
            }
        }
    }

    private static short getShort(ByteBuf buf, int offset) {
        return (short)(buf.getByte(offset) << 8 | buf.getByte(offset + 1) & 255);
    }

    private static SSLEngineResult unwrap(SSLEngine engine, ByteBuf in, ByteBuf out) throws SSLException {
        SSLEngineResult result;
        ByteBuffer in0 = in.nioBuffer();
        block3 : do {
            ByteBuffer out0 = out.nioBuffer(out.writerIndex(), out.writableBytes());
            result = engine.unwrap(in0, out0);
            in.skipBytes(result.bytesConsumed());
            out.writerIndex(out.writerIndex() + result.bytesProduced());
            switch (result.getStatus()) {
                case BUFFER_OVERFLOW: {
                    out.ensureWritableBytes(engine.getSession().getApplicationBufferSize());
                    continue block3;
                }
            }
            break;
        } while (true);
        return result;
    }

    private void runDelegatedTasks() {
        Runnable task;
        while ((task = this.engine.getDelegatedTask()) != null) {
            this.delegatedTaskExecutor.execute(task);
        }
    }

    private void setHandshakeSuccess() {
        ChannelFuture f;
        while ((f = this.handshakeFutures.poll()) != null) {
            f.setSuccess();
        }
    }

    private void setHandshakeFailure(Throwable cause) {
        ChannelFuture f;
        block4 : {
            this.engine.closeOutbound();
            try {
                this.engine.closeInbound();
            }
            catch (SSLException e) {
                if (!logger.isDebugEnabled()) break block4;
                logger.debug("SSLEngine.closeInbound() raised an exception after a handshake failure.", e);
            }
        }
        if (cause == null) {
            cause = new ClosedChannelException();
        }
        while ((f = this.handshakeFutures.poll()) != null) {
            f.setFailure(cause);
        }
        this.flush0(this.ctx, 0, cause);
    }

    private void closeOutboundAndChannel(ChannelHandlerContext ctx, ChannelFuture future, boolean disconnect) throws Exception {
        if (!ctx.channel().isActive()) {
            if (disconnect) {
                ctx.disconnect(future);
            } else {
                ctx.close(future);
            }
            return;
        }
        this.engine.closeOutbound();
        ChannelFuture closeNotifyFuture = ctx.newFuture();
        this.flush(ctx, closeNotifyFuture);
        this.safeClose(ctx, closeNotifyFuture, future);
    }

    @Override
    public void beforeAdd(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
    }

    @Override
    public void afterAdd(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isActive()) {
            this.handshake();
        }
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        if (!this.startTls && this.engine.getUseClientMode()) {
            this.handshake().addListener(new ChannelFutureListener(){

                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (!future.isSuccess()) {
                        ctx.pipeline().fireExceptionCaught(future.cause());
                        ctx.close();
                    }
                }
            });
        }
        ctx.fireChannelActive();
    }

    private void safeClose(final ChannelHandlerContext ctx, ChannelFuture flushFuture, final ChannelFuture closeFuture) {
        if (!ctx.channel().isActive()) {
            ctx.close(closeFuture);
            return;
        }
        final ScheduledFuture timeoutFuture = this.closeNotifyTimeoutMillis > 0 ? ctx.executor().schedule(new Runnable(){

            @Override
            public void run() {
                logger.warn(ctx.channel() + " last write attempt timed out." + " Force-closing the connection.");
                ctx.close(closeFuture);
            }
        }, this.closeNotifyTimeoutMillis, TimeUnit.MILLISECONDS) : null;
        flushFuture.addListener(new ChannelFutureListener(){

            @Override
            public void operationComplete(ChannelFuture f) throws Exception {
                if (timeoutFuture != null) {
                    timeoutFuture.cancel(false);
                }
                if (ctx.channel().isActive()) {
                    ctx.close(closeFuture);
                }
            }
        });
    }

    private final class SSLEngineInboundCloseFuture
    extends DefaultChannelFuture {
        public SSLEngineInboundCloseFuture() {
            super(null, true);
        }

        void setClosed() {
            super.setSuccess();
        }

        @Override
        public Channel channel() {
            if (SslHandler.this.ctx == null) {
                return null;
            }
            return SslHandler.this.ctx.channel();
        }

        @Override
        public boolean setSuccess() {
            return false;
        }

        @Override
        public boolean setFailure(Throwable cause) {
            return false;
        }
    }

}

