
package io.netty.handler.stream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ChannelBuf;
import io.netty.buffer.MessageBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundMessageHandler;
import io.netty.channel.EventExecutor;
import io.netty.handler.stream.ChunkedByteInput;
import io.netty.handler.stream.ChunkedInput;
import io.netty.handler.stream.ChunkedMessageInput;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicInteger;

public class ChunkedWriteHandler
extends ChannelHandlerAdapter
implements ChannelOutboundMessageHandler<Object> {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ChunkedWriteHandler.class);
    private final MessageBuf<Object> queue = Unpooled.messageBuffer();
    private final int maxPendingWrites;
    private volatile ChannelHandlerContext ctx;
    private final AtomicInteger pendingWrites = new AtomicInteger();
    private Object currentEvent;

    public ChunkedWriteHandler() {
        this(4);
    }

    public ChunkedWriteHandler(int maxPendingWrites) {
        if (maxPendingWrites <= 0) {
            throw new IllegalArgumentException("maxPendingWrites: " + maxPendingWrites + " (expected: > 0)");
        }
        this.maxPendingWrites = maxPendingWrites;
    }

    @Override
    public MessageBuf<Object> newOutboundBuffer(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        return this.queue;
    }

    @Override
    public void freeOutboundBuffer(ChannelHandlerContext ctx, ChannelBuf buf) throws Exception {
    }

    private boolean isWritable() {
        return this.pendingWrites.get() < this.maxPendingWrites;
    }

    public void resumeTransfer() {
        final ChannelHandlerContext ctx = this.ctx;
        if (ctx == null) {
            return;
        }
        if (ctx.executor().inEventLoop()) {
            try {
                this.doFlush(ctx);
            }
            catch (Exception e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Unexpected exception while sending chunks.", e);
                }
            }
        } else {
            ctx.executor().execute(new Runnable(){

                @Override
                public void run() {
                    block2 : {
                        try {
                            ChunkedWriteHandler.this.doFlush(ctx);
                        }
                        catch (Exception e) {
                            if (!logger.isWarnEnabled()) break block2;
                            logger.warn("Unexpected exception while sending chunks.", e);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void flush(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
        this.queue.add(future);
        if (this.isWritable() || !ctx.channel().isActive()) {
            this.doFlush(ctx);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.doFlush(ctx);
        super.channelInactive(ctx);
    }

    private void discard(ChannelHandlerContext ctx, Throwable cause) {
        boolean fireExceptionCaught = false;
        boolean success = true;
        do {
            Object currentEvent = this.currentEvent;
            if (this.currentEvent == null) {
                currentEvent = this.queue.poll();
            } else {
                this.currentEvent = null;
            }
            if (currentEvent == null) break;
            if (currentEvent instanceof ChunkedInput) {
                ChunkedInput in = (ChunkedInput)currentEvent;
                try {
                    if (!in.isEndOfInput()) {
                        success = false;
                    }
                }
                catch (Exception e) {
                    success = false;
                    logger.warn(ChunkedInput.class.getSimpleName() + ".isEndOfInput() failed", e);
                }
                ChunkedWriteHandler.closeInput(in);
                continue;
            }
            if (!(currentEvent instanceof ChannelFuture)) continue;
            ChannelFuture f = (ChannelFuture)currentEvent;
            if (!success) {
                fireExceptionCaught = true;
                if (cause == null) {
                    cause = new ClosedChannelException();
                }
                f.setFailure(cause);
                continue;
            }
            f.setSuccess();
        } while (true);
        if (fireExceptionCaught) {
            ctx.fireExceptionCaught(cause);
        }
    }

    private void doFlush(final ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        if (!channel.isActive()) {
            this.discard(ctx, null);
            return;
        }
        while (this.isWritable()) {
            if (this.currentEvent == null) {
                this.currentEvent = this.queue.poll();
            }
            if (this.currentEvent == null) break;
            final Object currentEvent = this.currentEvent;
            if (currentEvent instanceof ChannelFuture) {
                this.currentEvent = null;
                ctx.flush((ChannelFuture)currentEvent);
            } else if (currentEvent instanceof ChunkedInput) {
                boolean endOfInput;
                boolean suspend;
                final ChunkedInput chunks = (ChunkedInput)currentEvent;
                try {
                    boolean read = this.readChunk(ctx, chunks);
                    endOfInput = chunks.isEndOfInput();
                    suspend = !read ? !endOfInput : false;
                }
                catch (Throwable t) {
                    this.currentEvent = null;
                    if (ctx.executor().inEventLoop()) {
                        ctx.fireExceptionCaught(t);
                    } else {
                        ctx.executor().execute(new Runnable(){

                            @Override
                            public void run() {
                                ctx.fireExceptionCaught(t);
                            }
                        });
                    }
                    ChunkedWriteHandler.closeInput(chunks);
                    break;
                }
                if (suspend) break;
                this.pendingWrites.incrementAndGet();
                ChannelFuture f = ctx.flush();
                if (endOfInput) {
                    this.currentEvent = null;
                    f.addListener(new ChannelFutureListener(){

                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            ChunkedWriteHandler.this.pendingWrites.decrementAndGet();
                            ChunkedWriteHandler.closeInput(chunks);
                        }
                    });
                } else if (this.isWritable()) {
                    f.addListener(new ChannelFutureListener(){

                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            ChunkedWriteHandler.this.pendingWrites.decrementAndGet();
                            if (!future.isSuccess()) {
                                ChunkedWriteHandler.closeInput((ChunkedInput)currentEvent);
                            }
                        }
                    });
                } else {
                    f.addListener(new ChannelFutureListener(){

                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            ChunkedWriteHandler.this.pendingWrites.decrementAndGet();
                            if (!future.isSuccess()) {
                                ChunkedWriteHandler.closeInput((ChunkedInput)currentEvent);
                            } else if (ChunkedWriteHandler.this.isWritable()) {
                                ChunkedWriteHandler.this.resumeTransfer();
                            }
                        }
                    });
                }
            } else {
                ctx.nextOutboundMessageBuffer().add(currentEvent);
                this.currentEvent = null;
            }
            if (channel.isActive()) continue;
            this.discard(ctx, new ClosedChannelException());
            return;
        }
    }

    protected boolean readChunk(ChannelHandlerContext ctx, ChunkedInput<?> chunks) throws Exception {
        if (chunks instanceof ChunkedByteInput) {
            return ((ChunkedByteInput)chunks).readChunk(ctx.nextOutboundByteBuffer());
        }
        if (chunks instanceof ChunkedMessageInput) {
            return ((ChunkedMessageInput)chunks).readChunk(ctx.nextOutboundMessageBuffer());
        }
        throw new IllegalArgumentException("ChunkedInput instance " + chunks + " not supported");
    }

    static void closeInput(ChunkedInput<?> chunks) {
        block2 : {
            try {
                chunks.close();
            }
            catch (Throwable t) {
                if (!logger.isWarnEnabled()) break block2;
                logger.warn("Failed to close a chunked input.", t);
            }
        }
    }

    @Override
    public void beforeRemove(ChannelHandlerContext ctx) throws Exception {
        this.doFlush(ctx);
    }

    @Override
    public void afterRemove(ChannelHandlerContext ctx) throws Exception {
        this.discard(ctx, new ChannelException(ChunkedWriteHandler.class.getSimpleName() + " removed from pipeline."));
    }

}

