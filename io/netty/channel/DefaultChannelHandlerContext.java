
package io.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ChannelBuf;
import io.netty.buffer.MessageBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandlerType;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOperationHandler;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPipelineException;
import io.netty.channel.ChannelStateHandler;
import io.netty.channel.DefaultChannelPipeline;
import io.netty.channel.EventExecutor;
import io.netty.channel.EventExecutorGroup;
import io.netty.channel.EventLoop;
import io.netty.channel.FileRegion;
import io.netty.channel.NoSuchBufferException;
import io.netty.logging.InternalLogger;
import io.netty.util.DefaultAttributeMap;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

final class DefaultChannelHandlerContext
extends DefaultAttributeMap
implements ChannelHandlerContext {
    private static final EnumSet<ChannelHandlerType> EMPTY_TYPE = EnumSet.noneOf(ChannelHandlerType.class);
    static final int DIR_INBOUND = 1;
    static final int DIR_OUTBOUND = 2;
    private static final int FLAG_NEEDS_LAZY_INIT = 4;
    volatile DefaultChannelHandlerContext next;
    volatile DefaultChannelHandlerContext prev;
    private final Channel channel;
    private final DefaultChannelPipeline pipeline;
    private final String name;
    private final Set<ChannelHandlerType> type;
    private final ChannelHandler handler;
    final int flags;
    final AtomicBoolean readable = new AtomicBoolean(true);
    EventExecutor executor;
    private MessageBuf<Object> inMsgBuf;
    private ByteBuf inByteBuf;
    private MessageBuf<Object> outMsgBuf;
    private ByteBuf outByteBuf;
    private final AtomicReference<MessageBridge> inMsgBridge;
    AtomicReference<MessageBridge> outMsgBridge;
    private final AtomicReference<ByteBridge> inByteBridge;
    AtomicReference<ByteBridge> outByteBridge;
    private final Runnable fireChannelRegisteredTask;
    private final Runnable fireChannelUnregisteredTask;
    private final Runnable fireChannelActiveTask;
    private final Runnable fireChannelInactiveTask;
    private final Runnable curCtxFireInboundBufferUpdatedTask;
    private final Runnable nextCtxFireInboundBufferUpdatedTask;
    private final Runnable freeInboundBufferTask;
    private final Runnable freeOutboundBufferTask;

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    DefaultChannelHandlerContext(DefaultChannelPipeline pipeline, EventExecutorGroup group, DefaultChannelHandlerContext prev, DefaultChannelHandlerContext next, String name, ChannelHandler handler) {
        this.fireChannelRegisteredTask = new Runnable(){

            @Override
            public void run() {
                DefaultChannelHandlerContext ctx = DefaultChannelHandlerContext.this;
                try {
                    ((ChannelStateHandler)ctx.handler).channelRegistered(ctx);
                }
                catch (Throwable t) {
                    DefaultChannelHandlerContext.this.pipeline.notifyHandlerException(t);
                }
            }
        };
        this.fireChannelUnregisteredTask = new Runnable(){

            @Override
            public void run() {
                DefaultChannelHandlerContext ctx = DefaultChannelHandlerContext.this;
                try {
                    ((ChannelStateHandler)ctx.handler).channelUnregistered(ctx);
                }
                catch (Throwable t) {
                    DefaultChannelHandlerContext.this.pipeline.notifyHandlerException(t);
                }
            }
        };
        this.fireChannelActiveTask = new Runnable(){

            @Override
            public void run() {
                DefaultChannelHandlerContext ctx = DefaultChannelHandlerContext.this;
                try {
                    ((ChannelStateHandler)ctx.handler).channelActive(ctx);
                }
                catch (Throwable t) {
                    DefaultChannelHandlerContext.this.pipeline.notifyHandlerException(t);
                }
            }
        };
        this.fireChannelInactiveTask = new Runnable(){

            @Override
            public void run() {
                DefaultChannelHandlerContext ctx = DefaultChannelHandlerContext.this;
                try {
                    ((ChannelStateHandler)ctx.handler).channelInactive(ctx);
                }
                catch (Throwable t) {
                    DefaultChannelHandlerContext.this.pipeline.notifyHandlerException(t);
                }
            }
        };
        this.curCtxFireInboundBufferUpdatedTask = new Runnable(){

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void run() {
                DefaultChannelHandlerContext ctx = DefaultChannelHandlerContext.this;
                DefaultChannelHandlerContext.this.flushBridge();
                try {
                    ((ChannelStateHandler)ctx.handler).inboundBufferUpdated(ctx);
                }
                catch (Throwable t) {
                    DefaultChannelHandlerContext.this.pipeline.notifyHandlerException(t);
                }
                finally {
                    ByteBuf buf = DefaultChannelHandlerContext.this.inByteBuf;
                    if (buf != null && !buf.readable()) {
                        buf.discardReadBytes();
                    }
                }
            }
        };
        this.nextCtxFireInboundBufferUpdatedTask = new Runnable(){

            @Override
            public void run() {
                DefaultChannelHandlerContext next = DefaultChannelPipeline.nextContext(DefaultChannelHandlerContext.this.next, 1);
                if (next != null) {
                    next.fillBridge();
                    EventExecutor executor = next.executor();
                    if (executor.inEventLoop()) {
                        next.curCtxFireInboundBufferUpdatedTask.run();
                    } else {
                        executor.execute(next.curCtxFireInboundBufferUpdatedTask);
                    }
                }
            }
        };
        this.freeInboundBufferTask = new Runnable(){

            @Override
            public void run() {
                DefaultChannelHandlerContext nextCtx;
                DefaultChannelHandlerContext ctx = DefaultChannelHandlerContext.this;
                if (ctx.handler instanceof ChannelInboundHandler) {
                    ChannelInboundHandler h = (ChannelInboundHandler)ctx.handler;
                    try {
                        if (ctx.hasInboundByteBuffer()) {
                            if (ctx.inByteBuf != null) {
                                h.freeInboundBuffer(ctx, ctx.inByteBuf);
                            }
                        } else if (ctx.inMsgBuf != null) {
                            h.freeInboundBuffer(ctx, ctx.inMsgBuf);
                        }
                    }
                    catch (Throwable t) {
                        DefaultChannelHandlerContext.this.pipeline.notifyHandlerException(t);
                    }
                }
                if ((nextCtx = DefaultChannelPipeline.nextContext(ctx.next, 1)) != null) {
                    nextCtx.callFreeInboundBuffer();
                } else {
                    DefaultChannelHandlerContext.this.pipeline.firstContext(2).callFreeOutboundBuffer();
                }
            }
        };
        this.freeOutboundBufferTask = new Runnable(){

            @Override
            public void run() {
                DefaultChannelHandlerContext nextCtx;
                DefaultChannelHandlerContext ctx = DefaultChannelHandlerContext.this;
                if (ctx.handler instanceof ChannelOutboundHandler) {
                    ChannelOutboundHandler h = (ChannelOutboundHandler)ctx.handler;
                    try {
                        if (ctx.hasOutboundByteBuffer()) {
                            if (ctx.outByteBuf != null) {
                                h.freeOutboundBuffer(ctx, ctx.outByteBuf);
                            }
                        } else if (ctx.outMsgBuf != null) {
                            h.freeOutboundBuffer(ctx, ctx.outMsgBuf);
                        }
                    }
                    catch (Throwable t) {
                        DefaultChannelHandlerContext.this.pipeline.notifyHandlerException(t);
                    }
                }
                if ((nextCtx = DefaultChannelPipeline.nextContext(ctx.prev, 2)) != null) {
                    nextCtx.callFreeOutboundBuffer();
                }
            }
        };
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        int flags = 0;
        Object type = EMPTY_TYPE.clone();
        if (handler instanceof ChannelStateHandler) {
            type.add(ChannelHandlerType.STATE);
            flags |= true;
            if (handler instanceof ChannelInboundHandler) {
                type.add(ChannelHandlerType.INBOUND);
            }
        }
        if (handler instanceof ChannelOperationHandler) {
            type.add(ChannelHandlerType.OPERATION);
            flags |= 2;
            if (handler instanceof ChannelOutboundHandler) {
                type.add(ChannelHandlerType.OUTBOUND);
            }
        }
        this.type = Collections.unmodifiableSet(type);
        this.prev = prev;
        this.next = next;
        this.channel = pipeline.channel;
        this.pipeline = pipeline;
        this.name = name;
        this.handler = handler;
        if (group != null) {
            EventExecutor childExecutor = pipeline.childExecutors.get(group);
            if (childExecutor == null) {
                childExecutor = group.next();
                pipeline.childExecutors.put(group, childExecutor);
            }
            this.executor = childExecutor;
        } else {
            this.executor = this.channel.isRegistered() ? this.channel.eventLoop() : null;
        }
        if (type.contains((Object)ChannelHandlerType.INBOUND)) {
            ChannelBuf buf;
            try {
                buf = ((ChannelInboundHandler)handler).newInboundBuffer(this);
            }
            catch (Exception e) {
                throw new ChannelPipelineException("A user handler failed to create a new inbound buffer.", e);
            }
            if (buf == null) {
                throw new ChannelPipelineException("A user handler's newInboundBuffer() returned null");
            }
            if (buf instanceof ByteBuf) {
                this.inByteBuf = (ByteBuf)buf;
                this.inByteBridge = new AtomicReference();
                this.inMsgBuf = null;
                this.inMsgBridge = null;
            } else {
                if (!(buf instanceof MessageBuf)) throw new Error();
                this.inByteBuf = null;
                this.inByteBridge = null;
                this.inMsgBuf = (MessageBuf)buf;
                this.inMsgBridge = new AtomicReference();
            }
        } else {
            this.inByteBuf = null;
            this.inByteBridge = null;
            this.inMsgBuf = null;
            this.inMsgBridge = null;
        }
        if (type.contains((Object)ChannelHandlerType.OUTBOUND)) {
            if (prev == null) {
                flags |= 4;
            } else {
                this.initOutboundBuffer();
            }
        } else {
            this.outByteBuf = null;
            this.outByteBridge = null;
            this.outMsgBuf = null;
            this.outMsgBridge = null;
        }
        this.flags = flags;
    }

    private void lazyInitOutboundBuffer() {
        if ((this.flags & 4) != 0 && this.outByteBuf == null && this.outMsgBuf == null) {
            EventExecutor exec = this.executor();
            if (exec.inEventLoop()) {
                this.initOutboundBuffer();
            } else {
                try {
                    DefaultChannelHandlerContext.getFromFuture(exec.submit(new Runnable(){

                        @Override
                        public void run() {
                            DefaultChannelHandlerContext.this.lazyInitOutboundBuffer();
                        }
                    }));
                }
                catch (Exception e) {
                    throw new ChannelPipelineException("failed to initialize an outbound buffer lazily", e);
                }
            }
        }
    }

    private void initOutboundBuffer() {
        ChannelBuf buf;
        try {
            buf = ((ChannelOutboundHandler)this.handler).newOutboundBuffer(this);
        }
        catch (Exception e) {
            throw new ChannelPipelineException("A user handler failed to create a new outbound buffer.", e);
        }
        if (buf == null) {
            throw new ChannelPipelineException("A user handler's newOutboundBuffer() returned null");
        }
        if (buf instanceof ByteBuf) {
            this.outByteBuf = (ByteBuf)buf;
            this.outByteBridge = new AtomicReference();
            this.outMsgBuf = null;
            this.outMsgBridge = null;
        } else if (buf instanceof MessageBuf) {
            MessageBuf msgBuf;
            this.outByteBuf = null;
            this.outByteBridge = null;
            this.outMsgBuf = msgBuf = (MessageBuf)buf;
            this.outMsgBridge = new AtomicReference();
        } else {
            throw new Error();
        }
    }

    void fillBridge() {
        Object bridge;
        if (this.inMsgBridge != null) {
            bridge = this.inMsgBridge.get();
            if (bridge != null) {
                ((MessageBridge)bridge).fill();
            }
        } else if (this.inByteBridge != null && (bridge = this.inByteBridge.get()) != null) {
            ((ByteBridge)bridge).fill();
        }
        if (this.outMsgBridge != null) {
            bridge = this.outMsgBridge.get();
            if (bridge != null) {
                ((MessageBridge)bridge).fill();
            }
        } else if (this.outByteBridge != null && (bridge = this.outByteBridge.get()) != null) {
            ((ByteBridge)bridge).fill();
        }
    }

    void flushBridge() {
        Object bridge;
        if (this.inMsgBridge != null) {
            bridge = this.inMsgBridge.get();
            if (bridge != null) {
                ((MessageBridge)bridge).flush(this.inMsgBuf);
            }
        } else if (this.inByteBridge != null && (bridge = this.inByteBridge.get()) != null) {
            ((ByteBridge)bridge).flush(this.inByteBuf);
        }
        this.lazyInitOutboundBuffer();
        if (this.outMsgBridge != null) {
            bridge = this.outMsgBridge.get();
            if (bridge != null) {
                ((MessageBridge)bridge).flush(this.outMsgBuf);
            }
        } else if (this.outByteBridge != null && (bridge = this.outByteBridge.get()) != null) {
            ((ByteBridge)bridge).flush(this.outByteBuf);
        }
    }

    @Override
    public Channel channel() {
        return this.channel;
    }

    @Override
    public ChannelPipeline pipeline() {
        return this.pipeline;
    }

    @Override
    public ByteBufAllocator alloc() {
        return this.channel.config().getAllocator();
    }

    @Override
    public EventExecutor executor() {
        if (this.executor == null) {
            this.executor = this.channel.eventLoop();
            return this.executor;
        }
        return this.executor;
    }

    @Override
    public ChannelHandler handler() {
        return this.handler;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public Set<ChannelHandlerType> types() {
        return this.type;
    }

    @Override
    public boolean hasInboundByteBuffer() {
        return this.inByteBuf != null;
    }

    @Override
    public boolean hasInboundMessageBuffer() {
        return this.inMsgBuf != null;
    }

    @Override
    public ByteBuf inboundByteBuffer() {
        if (this.inByteBuf == null) {
            if (this.handler instanceof ChannelInboundHandler) {
                throw new NoSuchBufferException(String.format("the handler '%s' has no inbound byte buffer; it implements %s, but its newInboundBuffer() method created a %s.", this.name, ChannelInboundHandler.class.getSimpleName(), MessageBuf.class.getSimpleName()));
            }
            throw new NoSuchBufferException(String.format("the handler '%s' has no inbound byte buffer; it does not implement %s.", this.name, ChannelInboundHandler.class.getSimpleName()));
        }
        return this.inByteBuf;
    }

    @Override
    public <T> MessageBuf<T> inboundMessageBuffer() {
        if (this.inMsgBuf == null) {
            if (this.handler instanceof ChannelInboundHandler) {
                throw new NoSuchBufferException(String.format("the handler '%s' has no inbound message buffer; it implements %s, but its newInboundBuffer() method created a %s.", this.name, ChannelInboundHandler.class.getSimpleName(), ByteBuf.class.getSimpleName()));
            }
            throw new NoSuchBufferException(String.format("the handler '%s' has no inbound message buffer; it does not implement %s.", this.name, ChannelInboundHandler.class.getSimpleName()));
        }
        return this.inMsgBuf;
    }

    @Override
    public boolean hasOutboundByteBuffer() {
        this.lazyInitOutboundBuffer();
        return this.outByteBuf != null;
    }

    @Override
    public boolean hasOutboundMessageBuffer() {
        this.lazyInitOutboundBuffer();
        return this.outMsgBuf != null;
    }

    @Override
    public ByteBuf outboundByteBuffer() {
        if (this.outMsgBuf == null) {
            this.lazyInitOutboundBuffer();
        }
        if (this.outByteBuf == null) {
            if (this.handler instanceof ChannelOutboundHandler) {
                throw new NoSuchBufferException(String.format("the handler '%s' has no outbound byte buffer; it implements %s, but its newOutboundBuffer() method created a %s.", this.name, ChannelOutboundHandler.class.getSimpleName(), MessageBuf.class.getSimpleName()));
            }
            throw new NoSuchBufferException(String.format("the handler '%s' has no outbound byte buffer; it does not implement %s.", this.name, ChannelOutboundHandler.class.getSimpleName()));
        }
        return this.outByteBuf;
    }

    @Override
    public <T> MessageBuf<T> outboundMessageBuffer() {
        if (this.outMsgBuf == null) {
            this.initOutboundBuffer();
        }
        if (this.outMsgBuf == null) {
            if (this.handler instanceof ChannelOutboundHandler) {
                throw new NoSuchBufferException(String.format("the handler '%s' has no outbound message buffer; it implements %s, but its newOutboundBuffer() method created a %s.", this.name, ChannelOutboundHandler.class.getSimpleName(), ByteBuf.class.getSimpleName()));
            }
            throw new NoSuchBufferException(String.format("the handler '%s' has no outbound message buffer; it does not implement %s.", this.name, ChannelOutboundHandler.class.getSimpleName()));
        }
        return this.outMsgBuf;
    }

    private <T> T executeOnEventLoop(Callable<T> c) throws Exception {
        return DefaultChannelHandlerContext.getFromFuture(this.executor().submit(c));
    }

    void executeOnEventLoop(Runnable r) {
        DefaultChannelHandlerContext.waitForFuture(this.executor().submit(r));
    }

    private static <T> T getFromFuture(Future<T> future) throws Exception {
        try {
            return future.get();
        }
        catch (ExecutionException ex) {
            Throwable t = ex.getCause();
            if (t instanceof Error) {
                throw (Error)t;
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException)t;
            }
            if (t instanceof Exception) {
                throw (Exception)t;
            }
            throw new ChannelPipelineException(t);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    static void waitForFuture(Future<?> future) {
        try {
            future.get();
        }
        catch (ExecutionException ex) {
            Throwable t = ex.getCause();
            if (t instanceof Error) {
                throw (Error)t;
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException)t;
            }
            throw new ChannelPipelineException(t);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public ByteBuf replaceInboundByteBuffer(final ByteBuf newInboundByteBuf) {
        if (newInboundByteBuf == null) {
            throw new NullPointerException("newInboundByteBuf");
        }
        if (!this.executor().inEventLoop()) {
            try {
                return (ByteBuf)this.executeOnEventLoop(new Callable<ByteBuf>(){

                    @Override
                    public ByteBuf call() {
                        return DefaultChannelHandlerContext.this.replaceInboundByteBuffer(newInboundByteBuf);
                    }
                });
            }
            catch (Exception ex) {
                throw new ChannelPipelineException("failed to replace an inbound byte buffer", ex);
            }
        }
        ByteBuf currentInboundByteBuf = this.inboundByteBuffer();
        this.inByteBuf = newInboundByteBuf;
        return currentInboundByteBuf;
    }

    @Override
    public <T> MessageBuf<T> replaceInboundMessageBuffer(final MessageBuf<T> newInboundMsgBuf) {
        if (newInboundMsgBuf == null) {
            throw new NullPointerException("newInboundMsgBuf");
        }
        if (!this.executor().inEventLoop()) {
            try {
                return (MessageBuf)this.executeOnEventLoop(new Callable<MessageBuf<T>>(){

                    @Override
                    public MessageBuf<T> call() {
                        return DefaultChannelHandlerContext.this.replaceInboundMessageBuffer(newInboundMsgBuf);
                    }
                });
            }
            catch (Exception ex) {
                throw new ChannelPipelineException("failed to replace an inbound message buffer", ex);
            }
        }
        MessageBuf<T> currentInboundMsgBuf = this.inboundMessageBuffer();
        this.inMsgBuf = newInboundMsgBuf;
        return currentInboundMsgBuf;
    }

    @Override
    public ByteBuf replaceOutboundByteBuffer(final ByteBuf newOutboundByteBuf) {
        if (newOutboundByteBuf == null) {
            throw new NullPointerException("newOutboundByteBuf");
        }
        if (!this.executor().inEventLoop()) {
            try {
                return (ByteBuf)this.executeOnEventLoop(new Callable<ByteBuf>(){

                    @Override
                    public ByteBuf call() {
                        return DefaultChannelHandlerContext.this.replaceOutboundByteBuffer(newOutboundByteBuf);
                    }
                });
            }
            catch (Exception ex) {
                throw new ChannelPipelineException("failed to replace an outbound byte buffer", ex);
            }
        }
        ByteBuf currentOutboundByteBuf = this.outboundByteBuffer();
        this.outByteBuf = newOutboundByteBuf;
        return currentOutboundByteBuf;
    }

    @Override
    public <T> MessageBuf<T> replaceOutboundMessageBuffer(final MessageBuf<T> newOutboundMsgBuf) {
        if (newOutboundMsgBuf == null) {
            throw new NullPointerException("newOutboundMsgBuf");
        }
        if (!this.executor().inEventLoop()) {
            try {
                return (MessageBuf)this.executeOnEventLoop(new Callable<MessageBuf<T>>(){

                    @Override
                    public MessageBuf<T> call() {
                        return DefaultChannelHandlerContext.this.replaceOutboundMessageBuffer(newOutboundMsgBuf);
                    }
                });
            }
            catch (Exception ex) {
                throw new ChannelPipelineException("failed to replace an outbound message buffer", ex);
            }
        }
        MessageBuf<T> currentOutboundMsgBuf = this.outboundMessageBuffer();
        this.outMsgBuf = newOutboundMsgBuf;
        return currentOutboundMsgBuf;
    }

    @Override
    public boolean hasNextInboundByteBuffer() {
        DefaultChannelHandlerContext ctx = this.next;
        while (ctx != null) {
            if (ctx.inByteBridge != null) {
                return true;
            }
            ctx = ctx.next;
        }
        return false;
    }

    @Override
    public boolean hasNextInboundMessageBuffer() {
        DefaultChannelHandlerContext ctx = this.next;
        while (ctx != null) {
            if (ctx.inMsgBridge != null) {
                return true;
            }
            ctx = ctx.next;
        }
        return false;
    }

    @Override
    public boolean hasNextOutboundByteBuffer() {
        DefaultChannelHandlerContext ctx = this.prev;
        while (ctx != null) {
            ctx.lazyInitOutboundBuffer();
            if (ctx.outByteBridge != null) {
                return true;
            }
            ctx = ctx.prev;
        }
        return false;
    }

    @Override
    public boolean hasNextOutboundMessageBuffer() {
        DefaultChannelHandlerContext ctx = this.prev;
        while (ctx != null) {
            ctx.lazyInitOutboundBuffer();
            if (ctx.outMsgBridge != null) {
                return true;
            }
            ctx = ctx.prev;
        }
        return false;
    }

    @Override
    public ByteBuf nextInboundByteBuffer() {
        DefaultChannelHandlerContext ctx = this.next;
        Thread currentThread = Thread.currentThread();
        do {
            if (ctx == null) {
                if (this.prev != null) {
                    throw new NoSuchBufferException(String.format("the handler '%s' could not find a %s whose inbound buffer is %s.", this.name, ChannelInboundHandler.class.getSimpleName(), ByteBuf.class.getSimpleName()));
                }
                throw new NoSuchBufferException(String.format("the pipeline does not contain a %s whose inbound buffer is %s.", ChannelInboundHandler.class.getSimpleName(), ByteBuf.class.getSimpleName()));
            }
            if (ctx.inByteBuf != null) {
                if (ctx.executor().inEventLoop(currentThread)) {
                    return ctx.inByteBuf;
                }
                ByteBridge bridge = ctx.inByteBridge.get();
                if (bridge == null && !ctx.inByteBridge.compareAndSet((ByteBridge)null, bridge = new ByteBridge(ctx))) {
                    bridge = ctx.inByteBridge.get();
                }
                return bridge.byteBuf;
            }
            ctx = ctx.next;
        } while (true);
    }

    @Override
    public MessageBuf<Object> nextInboundMessageBuffer() {
        DefaultChannelHandlerContext ctx = this.next;
        Thread currentThread = Thread.currentThread();
        do {
            if (ctx == null) {
                if (this.prev != null) {
                    throw new NoSuchBufferException(String.format("the handler '%s' could not find a %s whose inbound buffer is %s.", this.name, ChannelInboundHandler.class.getSimpleName(), MessageBuf.class.getSimpleName()));
                }
                throw new NoSuchBufferException(String.format("the pipeline does not contain a %s whose inbound buffer is %s.", ChannelInboundHandler.class.getSimpleName(), MessageBuf.class.getSimpleName()));
            }
            if (ctx.inMsgBuf != null) {
                if (ctx.executor().inEventLoop(currentThread)) {
                    return ctx.inMsgBuf;
                }
                MessageBridge bridge = ctx.inMsgBridge.get();
                if (bridge == null && !ctx.inMsgBridge.compareAndSet((MessageBridge)null, bridge = new MessageBridge())) {
                    bridge = ctx.inMsgBridge.get();
                }
                return bridge.msgBuf;
            }
            ctx = ctx.next;
        } while (true);
    }

    @Override
    public ByteBuf nextOutboundByteBuffer() {
        return this.pipeline.nextOutboundByteBuffer(this.prev);
    }

    @Override
    public MessageBuf<Object> nextOutboundMessageBuffer() {
        return this.pipeline.nextOutboundMessageBuffer(this.prev);
    }

    @Override
    public void fireChannelRegistered() {
        DefaultChannelHandlerContext next = DefaultChannelPipeline.nextContext(this.next, 1);
        if (next != null) {
            EventExecutor executor = next.executor();
            if (executor.inEventLoop()) {
                next.fireChannelRegisteredTask.run();
            } else {
                executor.execute(next.fireChannelRegisteredTask);
            }
        }
    }

    @Override
    public void fireChannelUnregistered() {
        DefaultChannelHandlerContext next = DefaultChannelPipeline.nextContext(this.next, 1);
        if (next != null) {
            EventExecutor executor = next.executor();
            if (executor.inEventLoop()) {
                next.fireChannelUnregisteredTask.run();
            } else {
                executor.execute(next.fireChannelUnregisteredTask);
            }
        }
    }

    @Override
    public void fireChannelActive() {
        DefaultChannelHandlerContext next = DefaultChannelPipeline.nextContext(this.next, 1);
        if (next != null) {
            EventExecutor executor = next.executor();
            if (executor.inEventLoop()) {
                next.fireChannelActiveTask.run();
            } else {
                executor.execute(next.fireChannelActiveTask);
            }
        }
    }

    @Override
    public void fireChannelInactive() {
        DefaultChannelHandlerContext next = DefaultChannelPipeline.nextContext(this.next, 1);
        if (next != null) {
            EventExecutor executor = next.executor();
            if (executor.inEventLoop()) {
                next.fireChannelInactiveTask.run();
            } else {
                executor.execute(next.fireChannelInactiveTask);
            }
        }
    }

    @Override
    public void fireExceptionCaught(final Throwable cause) {
        block9 : {
            if (cause == null) {
                throw new NullPointerException("cause");
            }
            DefaultChannelHandlerContext next = this.next;
            if (next != null) {
                EventExecutor executor = next.executor();
                if (executor.inEventLoop()) {
                    try {
                        next.handler().exceptionCaught(next, cause);
                    }
                    catch (Throwable t) {
                        if (DefaultChannelPipeline.logger.isWarnEnabled()) {
                            DefaultChannelPipeline.logger.warn("An exception was thrown by a user handler's exceptionCaught() method while handling the following exception:", cause);
                        }
                        break block9;
                    }
                }
                try {
                    executor.execute(new Runnable(){

                        @Override
                        public void run() {
                            DefaultChannelHandlerContext.this.fireExceptionCaught(cause);
                        }
                    });
                }
                catch (Throwable t) {
                    if (DefaultChannelPipeline.logger.isWarnEnabled()) {
                        DefaultChannelPipeline.logger.warn("Failed to submit an exceptionCaught() event.", t);
                        DefaultChannelPipeline.logger.warn("The exceptionCaught() event that was failed to submit was:", cause);
                    }
                    break block9;
                }
            }
            DefaultChannelPipeline.logger.warn("An exceptionCaught() event was fired, and it reached at the end of the pipeline.  It usually means the last inbound handler in the pipeline did not handle the exception.", cause);
        }
    }

    @Override
    public void fireUserEventTriggered(final Object event) {
        if (event == null) {
            throw new NullPointerException("event");
        }
        DefaultChannelHandlerContext next = this.next;
        if (next != null) {
            EventExecutor executor = next.executor();
            if (executor.inEventLoop()) {
                try {
                    next.handler().userEventTriggered(next, event);
                }
                catch (Throwable t) {
                    this.pipeline.notifyHandlerException(t);
                }
            } else {
                executor.execute(new Runnable(){

                    @Override
                    public void run() {
                        DefaultChannelHandlerContext.this.fireUserEventTriggered(event);
                    }
                });
            }
        }
    }

    @Override
    public void fireInboundBufferUpdated() {
        EventExecutor executor = this.executor();
        if (executor.inEventLoop()) {
            this.nextCtxFireInboundBufferUpdatedTask.run();
        } else {
            executor.execute(this.nextCtxFireInboundBufferUpdatedTask);
        }
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        return this.bind(localAddress, this.newFuture());
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress) {
        return this.connect(remoteAddress, this.newFuture());
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
        return this.connect(remoteAddress, localAddress, this.newFuture());
    }

    @Override
    public ChannelFuture disconnect() {
        return this.disconnect(this.newFuture());
    }

    @Override
    public ChannelFuture close() {
        return this.close(this.newFuture());
    }

    @Override
    public ChannelFuture deregister() {
        return this.deregister(this.newFuture());
    }

    @Override
    public ChannelFuture flush() {
        return this.flush(this.newFuture());
    }

    @Override
    public ChannelFuture write(Object message) {
        return this.write(message, this.newFuture());
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress, ChannelFuture future) {
        return this.pipeline.bind(DefaultChannelPipeline.nextContext(this.prev, 2), localAddress, future);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, ChannelFuture future) {
        return this.connect(remoteAddress, null, future);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelFuture future) {
        return this.pipeline.connect(DefaultChannelPipeline.nextContext(this.prev, 2), remoteAddress, localAddress, future);
    }

    @Override
    public ChannelFuture disconnect(ChannelFuture future) {
        return this.pipeline.disconnect(DefaultChannelPipeline.nextContext(this.prev, 2), future);
    }

    @Override
    public ChannelFuture close(ChannelFuture future) {
        return this.pipeline.close(DefaultChannelPipeline.nextContext(this.prev, 2), future);
    }

    @Override
    public ChannelFuture deregister(ChannelFuture future) {
        return this.pipeline.deregister(DefaultChannelPipeline.nextContext(this.prev, 2), future);
    }

    @Override
    public ChannelFuture flush(final ChannelFuture future) {
        EventExecutor executor = this.executor();
        if (executor.inEventLoop()) {
            DefaultChannelHandlerContext prev = DefaultChannelPipeline.nextContext(this.prev, 2);
            prev.fillBridge();
            this.pipeline.flush(prev, future);
        } else {
            executor.execute(new Runnable(){

                @Override
                public void run() {
                    DefaultChannelHandlerContext.this.flush(future);
                }
            });
        }
        return future;
    }

    @Override
    public ChannelFuture write(Object message, ChannelFuture future) {
        return this.pipeline.write(this.prev, message, future);
    }

    void callFreeInboundBuffer() {
        EventExecutor executor = this.executor();
        if (executor.inEventLoop()) {
            this.freeInboundBufferTask.run();
        } else {
            executor.execute(this.freeInboundBufferTask);
        }
    }

    private void callFreeOutboundBuffer() {
        EventExecutor executor = this.executor();
        if (executor.inEventLoop()) {
            this.freeOutboundBufferTask.run();
        } else {
            executor.execute(this.freeOutboundBufferTask);
        }
    }

    @Override
    public ChannelFuture newFuture() {
        return this.channel.newFuture();
    }

    @Override
    public ChannelFuture newSucceededFuture() {
        return this.channel.newSucceededFuture();
    }

    @Override
    public ChannelFuture newFailedFuture(Throwable cause) {
        return this.channel.newFailedFuture(cause);
    }

    @Override
    public boolean isReadable() {
        return this.readable.get();
    }

    @Override
    public void readable(boolean readable) {
        this.pipeline.readable(this, readable);
    }

    @Override
    public ChannelFuture sendFile(FileRegion region) {
        return this.pipeline.sendFile(DefaultChannelPipeline.nextContext(this.prev, 2), region, this.newFuture());
    }

    @Override
    public ChannelFuture sendFile(FileRegion region, ChannelFuture future) {
        return this.pipeline.sendFile(DefaultChannelPipeline.nextContext(this.prev, 2), region, future);
    }

    static final class ByteBridge {
        final ByteBuf byteBuf;
        private final Queue<ByteBuf> exchangeBuf = new ConcurrentLinkedQueue<ByteBuf>();
        private final ChannelHandlerContext ctx;

        ByteBridge(ChannelHandlerContext ctx) {
            this.ctx = ctx;
            this.byteBuf = ctx.alloc().buffer();
        }

        private void fill() {
            if (!this.byteBuf.readable()) {
                return;
            }
            int dataLen = this.byteBuf.readableBytes();
            ByteBuf data = this.byteBuf.isDirect() ? this.ctx.alloc().directBuffer(dataLen, dataLen) : this.ctx.alloc().buffer(dataLen, dataLen);
            this.byteBuf.readBytes(data, dataLen);
            this.byteBuf.unsafe().discardSomeReadBytes();
            this.exchangeBuf.add(data);
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        private void flush(ByteBuf out) {
            ByteBuf data;
            while (out.writable() && (data = this.exchangeBuf.peek()) != null) {
                if (out.writerIndex() > out.maxCapacity() - data.readableBytes()) {
                    out.ensureWritableBytes(out.maxCapacity() - out.writerIndex());
                    out.writeBytes(data, out.writableBytes());
                    continue;
                }
                this.exchangeBuf.remove();
                try {
                    out.writeBytes(data);
                    continue;
                }
                finally {
                    data.unsafe().free();
                    continue;
                }
            }
        }
    }

    static final class MessageBridge {
        final MessageBuf<Object> msgBuf = Unpooled.messageBuffer();
        private final Queue<Object[]> exchangeBuf = new ConcurrentLinkedQueue<Object[]>();

        MessageBridge() {
        }

        private void fill() {
            if (this.msgBuf.isEmpty()) {
                return;
            }
            Object[] data = this.msgBuf.toArray();
            this.msgBuf.clear();
            this.exchangeBuf.add(data);
        }

        private void flush(MessageBuf<Object> out) {
            Object[] data;
            while ((data = this.exchangeBuf.poll()) != null) {
                Collections.addAll(out, data);
            }
        }
    }

}

