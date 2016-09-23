
package io.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ChannelBuf;
import io.netty.buffer.ChannelBufType;
import io.netty.buffer.MessageBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandlerLifeCycleException;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOperationHandler;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPipelineException;
import io.netty.channel.ChannelStateHandlerAdapter;
import io.netty.channel.DefaultChannelHandlerContext;
import io.netty.channel.EventExecutor;
import io.netty.channel.EventExecutorGroup;
import io.netty.channel.FileRegion;
import io.netty.channel.NoSuchBufferException;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultChannelPipeline
implements ChannelPipeline {
    static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultChannelPipeline.class);
    final Channel channel;
    private final Channel.Unsafe unsafe;
    final DefaultChannelHandlerContext head;
    private volatile DefaultChannelHandlerContext tail;
    private final Map<String, DefaultChannelHandlerContext> name2ctx = new HashMap<String, DefaultChannelHandlerContext>(4);
    private boolean firedChannelActive;
    private boolean fireInboundBufferUpdatedOnActivation;
    final Map<EventExecutorGroup, EventExecutor> childExecutors = new IdentityHashMap<EventExecutorGroup, EventExecutor>();
    private final AtomicInteger suspendRead = new AtomicInteger();

    public DefaultChannelPipeline(Channel channel) {
        if (channel == null) {
            throw new NullPointerException("channel");
        }
        this.channel = channel;
        HeadHandler headHandler = new HeadHandler();
        this.tail = this.head = new DefaultChannelHandlerContext(this, null, null, null, DefaultChannelPipeline.generateName(headHandler), headHandler);
        this.unsafe = channel.unsafe();
    }

    @Override
    public Channel channel() {
        return this.channel;
    }

    @Override
    public ChannelPipeline addFirst(String name, ChannelHandler handler) {
        return this.addFirst(null, name, handler);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public ChannelPipeline addFirst(EventExecutorGroup group, final String name, ChannelHandler handler) {
        DefaultChannelHandlerContext newCtx;
        DefaultChannelHandlerContext nextCtx;
        DefaultChannelPipeline defaultChannelPipeline = this;
        synchronized (defaultChannelPipeline) {
            this.checkDuplicateName(name);
            nextCtx = this.head.next;
            newCtx = new DefaultChannelHandlerContext(this, group, this.head, nextCtx, name, handler);
            if (!newCtx.channel().isRegistered() || newCtx.executor().inEventLoop()) {
                this.addFirst0(name, nextCtx, newCtx);
                return this;
            }
        }
        newCtx.executeOnEventLoop(new Runnable(){

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void run() {
                DefaultChannelPipeline defaultChannelPipeline = DefaultChannelPipeline.this;
                synchronized (defaultChannelPipeline) {
                    DefaultChannelPipeline.this.checkDuplicateName(name);
                    DefaultChannelPipeline.this.addFirst0(name, nextCtx, newCtx);
                }
            }
        });
        return this;
    }

    private void addFirst0(String name, DefaultChannelHandlerContext nextCtx, DefaultChannelHandlerContext newCtx) {
        DefaultChannelPipeline.callBeforeAdd(newCtx);
        if (nextCtx != null) {
            nextCtx.prev = newCtx;
        }
        this.head.next = newCtx;
        if (this.tail == this.head) {
            this.tail = newCtx;
        }
        this.name2ctx.put(name, newCtx);
        this.callAfterAdd(newCtx);
    }

    @Override
    public ChannelPipeline addLast(String name, ChannelHandler handler) {
        return this.addLast(null, name, handler);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public ChannelPipeline addLast(EventExecutorGroup group, final String name, ChannelHandler handler) {
        DefaultChannelHandlerContext oldTail;
        DefaultChannelHandlerContext newTail;
        DefaultChannelPipeline defaultChannelPipeline = this;
        synchronized (defaultChannelPipeline) {
            this.checkDuplicateName(name);
            oldTail = this.tail;
            newTail = new DefaultChannelHandlerContext(this, group, oldTail, null, name, handler);
            if (!newTail.channel().isRegistered() || newTail.executor().inEventLoop()) {
                this.addLast0(name, oldTail, newTail);
                return this;
            }
        }
        newTail.executeOnEventLoop(new Runnable(){

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void run() {
                DefaultChannelPipeline defaultChannelPipeline = DefaultChannelPipeline.this;
                synchronized (defaultChannelPipeline) {
                    DefaultChannelPipeline.this.checkDuplicateName(name);
                    DefaultChannelPipeline.this.addLast0(name, oldTail, newTail);
                }
            }
        });
        return this;
    }

    private void addLast0(String name, DefaultChannelHandlerContext oldTail, DefaultChannelHandlerContext newTail) {
        DefaultChannelPipeline.callBeforeAdd(newTail);
        oldTail.next = newTail;
        this.tail = newTail;
        this.name2ctx.put(name, newTail);
        this.callAfterAdd(newTail);
    }

    @Override
    public ChannelPipeline addBefore(String baseName, String name, ChannelHandler handler) {
        return this.addBefore(null, baseName, name, handler);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public ChannelPipeline addBefore(EventExecutorGroup group, String baseName, final String name, ChannelHandler handler) {
        DefaultChannelHandlerContext newCtx;
        DefaultChannelHandlerContext ctx;
        DefaultChannelPipeline defaultChannelPipeline = this;
        synchronized (defaultChannelPipeline) {
            ctx = this.getContextOrDie(baseName);
            this.checkDuplicateName(name);
            newCtx = new DefaultChannelHandlerContext(this, group, ctx.prev, ctx, name, handler);
            if (!newCtx.channel().isRegistered() || newCtx.executor().inEventLoop()) {
                this.addBefore0(name, ctx, newCtx);
                return this;
            }
        }
        newCtx.executeOnEventLoop(new Runnable(){

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void run() {
                DefaultChannelPipeline defaultChannelPipeline = DefaultChannelPipeline.this;
                synchronized (defaultChannelPipeline) {
                    DefaultChannelPipeline.this.checkDuplicateName(name);
                    DefaultChannelPipeline.this.addBefore0(name, ctx, newCtx);
                }
            }
        });
        return this;
    }

    private void addBefore0(String name, DefaultChannelHandlerContext ctx, DefaultChannelHandlerContext newCtx) {
        DefaultChannelPipeline.callBeforeAdd(newCtx);
        ctx.prev.next = newCtx;
        ctx.prev = newCtx;
        this.name2ctx.put(name, newCtx);
        this.callAfterAdd(newCtx);
    }

    @Override
    public ChannelPipeline addAfter(String baseName, String name, ChannelHandler handler) {
        return this.addAfter(null, baseName, name, handler);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public ChannelPipeline addAfter(EventExecutorGroup group, String baseName, final String name, ChannelHandler handler) {
        DefaultChannelHandlerContext newCtx;
        DefaultChannelHandlerContext ctx;
        DefaultChannelPipeline defaultChannelPipeline = this;
        synchronized (defaultChannelPipeline) {
            ctx = this.getContextOrDie(baseName);
            if (ctx == this.tail) {
                return this.addLast(name, handler);
            }
            this.checkDuplicateName(name);
            newCtx = new DefaultChannelHandlerContext(this, group, ctx, ctx.next, name, handler);
            if (!newCtx.channel().isRegistered() || newCtx.executor().inEventLoop()) {
                this.addAfter0(name, ctx, newCtx);
                return this;
            }
        }
        newCtx.executeOnEventLoop(new Runnable(){

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void run() {
                DefaultChannelPipeline defaultChannelPipeline = DefaultChannelPipeline.this;
                synchronized (defaultChannelPipeline) {
                    DefaultChannelPipeline.this.checkDuplicateName(name);
                    DefaultChannelPipeline.this.addAfter0(name, ctx, newCtx);
                }
            }
        });
        return this;
    }

    private void addAfter0(String name, DefaultChannelHandlerContext ctx, DefaultChannelHandlerContext newCtx) {
        this.checkDuplicateName(name);
        DefaultChannelPipeline.callBeforeAdd(newCtx);
        ctx.next.prev = newCtx;
        ctx.next = newCtx;
        this.name2ctx.put(name, newCtx);
        this.callAfterAdd(newCtx);
    }

    @Override
    public /* varargs */ ChannelPipeline addFirst(ChannelHandler ... handlers) {
        return this.addFirst((EventExecutorGroup)null, handlers);
    }

    @Override
    public /* varargs */ ChannelPipeline addFirst(EventExecutorGroup executor, ChannelHandler ... handlers) {
        if (handlers == null) {
            throw new NullPointerException("handlers");
        }
        if (handlers.length == 0 || handlers[0] == null) {
            return this;
        }
        for (int size = 1; size < handlers.length && handlers[size] != null; ++size) {
        }
        for (int i = size - 1; i >= 0; --i) {
            ChannelHandler h = handlers[i];
            this.addFirst(executor, DefaultChannelPipeline.generateName(h), h);
        }
        return this;
    }

    @Override
    public /* varargs */ ChannelPipeline addLast(ChannelHandler ... handlers) {
        return this.addLast((EventExecutorGroup)null, handlers);
    }

    @Override
    public /* varargs */ ChannelPipeline addLast(EventExecutorGroup executor, ChannelHandler ... handlers) {
        if (handlers == null) {
            throw new NullPointerException("handlers");
        }
        for (ChannelHandler h : handlers) {
            if (h == null) break;
            this.addLast(executor, DefaultChannelPipeline.generateName(h), h);
        }
        return this;
    }

    private static String generateName(ChannelHandler handler) {
        String type = handler.getClass().getSimpleName();
        StringBuilder buf = new StringBuilder(type.length() + 10);
        buf.append(type);
        buf.append("-0");
        buf.append(Long.toHexString((long)System.identityHashCode(handler) & 0xFFFFFFFFL | 0x100000000L));
        buf.setCharAt(buf.length() - 9, 'x');
        return buf.toString();
    }

    @Override
    public void remove(ChannelHandler handler) {
        this.remove(this.getContextOrDie(handler));
    }

    @Override
    public ChannelHandler remove(String name) {
        return this.remove(this.getContextOrDie(name)).handler();
    }

    @Override
    public <T extends ChannelHandler> T remove(Class<T> handlerType) {
        return (T)this.remove(this.getContextOrDie(handlerType)).handler();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private DefaultChannelHandlerContext remove(final DefaultChannelHandlerContext ctx) {
        DefaultChannelHandlerContext context;
        Future future;
        DefaultChannelPipeline defaultChannelPipeline = this;
        synchronized (defaultChannelPipeline) {
            if (this.head == this.tail) {
                return null;
            }
            if (ctx == this.head) {
                throw new Error();
            }
            if (ctx == this.tail) {
                if (this.head == this.tail) {
                    throw new NoSuchElementException();
                }
                final DefaultChannelHandlerContext oldTail = this.tail;
                if (!oldTail.channel().isRegistered() || oldTail.executor().inEventLoop()) {
                    this.removeLast0(oldTail);
                    return oldTail;
                }
                future = oldTail.executor().submit(new Runnable(){

                    /*
                     * WARNING - Removed try catching itself - possible behaviour change.
                     */
                    @Override
                    public void run() {
                        DefaultChannelPipeline defaultChannelPipeline = DefaultChannelPipeline.this;
                        synchronized (defaultChannelPipeline) {
                            DefaultChannelPipeline.this.removeLast0(oldTail);
                        }
                    }
                });
                context = oldTail;
            } else {
                if (!ctx.channel().isRegistered() || ctx.executor().inEventLoop()) {
                    this.remove0(ctx);
                    return ctx;
                }
                future = ctx.executor().submit(new Runnable(){

                    /*
                     * WARNING - Removed try catching itself - possible behaviour change.
                     */
                    @Override
                    public void run() {
                        DefaultChannelPipeline defaultChannelPipeline = DefaultChannelPipeline.this;
                        synchronized (defaultChannelPipeline) {
                            DefaultChannelPipeline.this.remove0(ctx);
                        }
                    }
                });
                context = ctx;
            }
        }
        DefaultChannelHandlerContext.waitForFuture(future);
        return context;
    }

    private void remove0(DefaultChannelHandlerContext ctx) {
        DefaultChannelHandlerContext next;
        DefaultChannelPipeline.callBeforeRemove(ctx);
        DefaultChannelHandlerContext prev = ctx.prev;
        prev.next = next = ctx.next;
        next.prev = prev;
        this.name2ctx.remove(ctx.name());
        DefaultChannelPipeline.callAfterRemove(ctx);
        ctx.readable(true);
    }

    @Override
    public ChannelHandler removeFirst() {
        if (this.head == this.tail) {
            throw new NoSuchElementException();
        }
        return this.remove(this.head.next).handler();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public ChannelHandler removeLast() {
        DefaultChannelHandlerContext oldTail;
        DefaultChannelPipeline defaultChannelPipeline = this;
        synchronized (defaultChannelPipeline) {
            if (this.head == this.tail) {
                throw new NoSuchElementException();
            }
            oldTail = this.tail;
            if (!oldTail.channel().isRegistered() || oldTail.executor().inEventLoop()) {
                this.removeLast0(oldTail);
                return oldTail.handler();
            }
        }
        oldTail.executeOnEventLoop(new Runnable(){

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void run() {
                DefaultChannelPipeline defaultChannelPipeline = DefaultChannelPipeline.this;
                synchronized (defaultChannelPipeline) {
                    DefaultChannelPipeline.this.removeLast0(oldTail);
                }
            }
        });
        return oldTail.handler();
    }

    private void removeLast0(DefaultChannelHandlerContext oldTail) {
        DefaultChannelPipeline.callBeforeRemove(oldTail);
        oldTail.prev.next = null;
        this.tail = oldTail.prev;
        this.name2ctx.remove(oldTail.name());
        DefaultChannelPipeline.callBeforeRemove(oldTail);
        oldTail.readable(true);
    }

    @Override
    public void replace(ChannelHandler oldHandler, String newName, ChannelHandler newHandler) {
        this.replace(this.getContextOrDie(oldHandler), newName, newHandler);
    }

    @Override
    public ChannelHandler replace(String oldName, String newName, ChannelHandler newHandler) {
        return this.replace(this.getContextOrDie(oldName), newName, newHandler);
    }

    @Override
    public <T extends ChannelHandler> T replace(Class<T> oldHandlerType, String newName, ChannelHandler newHandler) {
        return (T)this.replace(this.getContextOrDie(oldHandlerType), newName, newHandler);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private ChannelHandler replace(final DefaultChannelHandlerContext ctx, final String newName, ChannelHandler newHandler) {
        Future future;
        DefaultChannelPipeline defaultChannelPipeline = this;
        synchronized (defaultChannelPipeline) {
            if (ctx == this.head) {
                throw new IllegalArgumentException();
            }
            if (ctx == this.tail) {
                if (this.head == this.tail) {
                    throw new NoSuchElementException();
                }
                final DefaultChannelHandlerContext oldTail = this.tail;
                final DefaultChannelHandlerContext newTail = new DefaultChannelHandlerContext(this, null, oldTail, null, newName, newHandler);
                if (!oldTail.channel().isRegistered() || oldTail.executor().inEventLoop()) {
                    this.removeLast0(oldTail);
                    this.checkDuplicateName(newName);
                    this.addLast0(newName, this.tail, newTail);
                    return ctx.handler();
                }
                future = oldTail.executor().submit(new Runnable(){

                    /*
                     * WARNING - Removed try catching itself - possible behaviour change.
                     */
                    @Override
                    public void run() {
                        DefaultChannelPipeline defaultChannelPipeline = DefaultChannelPipeline.this;
                        synchronized (defaultChannelPipeline) {
                            DefaultChannelPipeline.this.removeLast0(oldTail);
                            DefaultChannelPipeline.this.checkDuplicateName(newName);
                            DefaultChannelPipeline.this.addLast0(newName, DefaultChannelPipeline.this.tail, newTail);
                        }
                    }
                });
            } else {
                DefaultChannelHandlerContext newCtx;
                DefaultChannelHandlerContext next;
                DefaultChannelHandlerContext prev;
                boolean sameName = ctx.name().equals(newName);
                if (!sameName) {
                    this.checkDuplicateName(newName);
                }
                if (!(newCtx = new DefaultChannelHandlerContext(this, ctx.executor, prev = ctx.prev, next = ctx.next, newName, newHandler)).channel().isRegistered() || newCtx.executor().inEventLoop()) {
                    this.replace0(ctx, newName, newCtx);
                    return ctx.handler();
                }
                future = newCtx.executor().submit(new Runnable(){

                    /*
                     * WARNING - Removed try catching itself - possible behaviour change.
                     */
                    @Override
                    public void run() {
                        DefaultChannelPipeline defaultChannelPipeline = DefaultChannelPipeline.this;
                        synchronized (defaultChannelPipeline) {
                            DefaultChannelPipeline.this.replace0(ctx, newName, newCtx);
                        }
                    }
                });
            }
        }
        DefaultChannelHandlerContext.waitForFuture(future);
        return ctx.handler();
    }

    private void replace0(DefaultChannelHandlerContext ctx, String newName, DefaultChannelHandlerContext newCtx) {
        boolean sameName = ctx.name().equals(newName);
        DefaultChannelHandlerContext prev = ctx.prev;
        DefaultChannelHandlerContext next = ctx.next;
        DefaultChannelPipeline.callBeforeRemove(ctx);
        DefaultChannelPipeline.callBeforeAdd(newCtx);
        prev.next = newCtx;
        next.prev = newCtx;
        if (!sameName) {
            this.name2ctx.remove(ctx.name());
        }
        this.name2ctx.put(newName, newCtx);
        ChannelHandlerLifeCycleException removeException = null;
        ChannelHandlerLifeCycleException addException = null;
        boolean removed = false;
        try {
            DefaultChannelPipeline.callAfterRemove(ctx);
            ctx.readable(true);
            removed = true;
        }
        catch (ChannelHandlerLifeCycleException e) {
            removeException = e;
        }
        boolean added = false;
        try {
            this.callAfterAdd(newCtx);
            added = true;
        }
        catch (ChannelHandlerLifeCycleException e) {
            addException = e;
        }
        if (!removed && !added) {
            logger.warn(removeException.getMessage(), removeException);
            logger.warn(addException.getMessage(), addException);
            throw new ChannelHandlerLifeCycleException("Both " + ctx.handler().getClass().getName() + ".afterRemove() and " + newCtx.handler().getClass().getName() + ".afterAdd() failed; see logs.");
        }
        if (!removed) {
            throw removeException;
        }
        if (!added) {
            throw addException;
        }
    }

    private static void callBeforeAdd(ChannelHandlerContext ctx) {
        ChannelHandler handler = ctx.handler();
        if (handler instanceof ChannelStateHandlerAdapter) {
            ChannelStateHandlerAdapter h = (ChannelStateHandlerAdapter)handler;
            if (!h.isSharable() && h.added) {
                throw new ChannelHandlerLifeCycleException(h.getClass().getName() + " is not a @Sharable handler, so can't be added or removed multiple times.");
            }
            h.added = true;
        }
        try {
            handler.beforeAdd(ctx);
        }
        catch (Throwable t) {
            throw new ChannelHandlerLifeCycleException(handler.getClass().getName() + ".beforeAdd() has thrown an exception; not adding.", t);
        }
    }

    private void callAfterAdd(ChannelHandlerContext ctx) {
        try {
            ctx.handler().afterAdd(ctx);
        }
        catch (Throwable t) {
            boolean removed;
            block5 : {
                removed = false;
                try {
                    this.remove((DefaultChannelHandlerContext)ctx);
                    removed = true;
                }
                catch (Throwable t2) {
                    if (!logger.isWarnEnabled()) break block5;
                    logger.warn("Failed to remove a handler: " + ctx.name(), t2);
                }
            }
            if (removed) {
                throw new ChannelHandlerLifeCycleException(ctx.handler().getClass().getName() + ".afterAdd() has thrown an exception; removed.", t);
            }
            throw new ChannelHandlerLifeCycleException(ctx.handler().getClass().getName() + ".afterAdd() has thrown an exception; also failed to remove.", t);
        }
    }

    private static void callBeforeRemove(ChannelHandlerContext ctx) {
        try {
            ctx.handler().beforeRemove(ctx);
        }
        catch (Throwable t) {
            throw new ChannelHandlerLifeCycleException(ctx.handler().getClass().getName() + ".beforeRemove() has thrown an exception; not removing.", t);
        }
    }

    private static void callAfterRemove(ChannelHandlerContext ctx) {
        try {
            ctx.handler().afterRemove(ctx);
        }
        catch (Throwable t) {
            throw new ChannelHandlerLifeCycleException(ctx.handler().getClass().getName() + ".afterRemove() has thrown an exception.", t);
        }
    }

    @Override
    public ChannelHandler first() {
        DefaultChannelHandlerContext first = this.head.next;
        if (first == null) {
            return null;
        }
        return first.handler();
    }

    @Override
    public ChannelHandlerContext firstContext() {
        return this.head.next;
    }

    @Override
    public ChannelHandler last() {
        DefaultChannelHandlerContext last = this.tail;
        if (last == this.head || last == null) {
            return null;
        }
        return last.handler();
    }

    @Override
    public ChannelHandlerContext lastContext() {
        DefaultChannelHandlerContext last = this.tail;
        if (last == this.head || last == null) {
            return null;
        }
        return last;
    }

    @Override
    public ChannelHandler get(String name) {
        ChannelHandlerContext ctx = this.context(name);
        if (ctx == null) {
            return null;
        }
        return ctx.handler();
    }

    @Override
    public <T extends ChannelHandler> T get(Class<T> handlerType) {
        ChannelHandlerContext ctx = this.context(handlerType);
        if (ctx == null) {
            return null;
        }
        return (T)ctx.handler();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public ChannelHandlerContext context(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        DefaultChannelPipeline defaultChannelPipeline = this;
        synchronized (defaultChannelPipeline) {
            return this.name2ctx.get(name);
        }
    }

    @Override
    public ChannelHandlerContext context(ChannelHandler handler) {
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        DefaultChannelHandlerContext ctx = this.head.next;
        while (ctx != null) {
            if (ctx.handler() == handler) {
                return ctx;
            }
            ctx = ctx.next;
        }
        return null;
    }

    @Override
    public ChannelHandlerContext context(Class<? extends ChannelHandler> handlerType) {
        if (handlerType == null) {
            throw new NullPointerException("handlerType");
        }
        DefaultChannelHandlerContext ctx = this.head.next;
        while (ctx != null) {
            if (handlerType.isAssignableFrom(ctx.handler().getClass())) {
                return ctx;
            }
            ctx = ctx.next;
        }
        return null;
    }

    @Override
    public List<String> names() {
        ArrayList<String> list = new ArrayList<String>();
        DefaultChannelHandlerContext ctx = this.head.next;
        while (ctx != null) {
            list.add(ctx.name());
            ctx = ctx.next;
        }
        return list;
    }

    @Override
    public Map<String, ChannelHandler> toMap() {
        LinkedHashMap<String, ChannelHandler> map = new LinkedHashMap<String, ChannelHandler>();
        DefaultChannelHandlerContext ctx = this.head.next;
        while (ctx != null) {
            map.put(ctx.name(), ctx.handler());
            ctx = ctx.next;
        }
        return map;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(this.getClass().getSimpleName());
        buf.append('{');
        DefaultChannelHandlerContext ctx = this.head.next;
        while (ctx != null) {
            buf.append('(');
            buf.append(ctx.name());
            buf.append(" = ");
            buf.append(ctx.handler().getClass().getName());
            buf.append(')');
            ctx = ctx.next;
            if (ctx == null) break;
            buf.append(", ");
        }
        buf.append('}');
        return buf.toString();
    }

    @Override
    public <T> MessageBuf<T> inboundMessageBuffer() {
        return this.head.nextInboundMessageBuffer();
    }

    @Override
    public ByteBuf inboundByteBuffer() {
        return this.head.nextInboundByteBuffer();
    }

    @Override
    public <T> MessageBuf<T> outboundMessageBuffer() {
        return this.nextOutboundMessageBuffer(this.tail);
    }

    @Override
    public ByteBuf outboundByteBuffer() {
        return this.nextOutboundByteBuffer(this.tail);
    }

    ByteBuf nextOutboundByteBuffer(DefaultChannelHandlerContext ctx) {
        DefaultChannelHandlerContext initialCtx = ctx;
        Thread currentThread = Thread.currentThread();
        do {
            if (ctx == null) {
                if (initialCtx != null && initialCtx.next != null) {
                    throw new NoSuchBufferException(String.format("the handler '%s' could not find a %s whose outbound buffer is %s.", initialCtx.next.name(), ChannelOutboundHandler.class.getSimpleName(), ByteBuf.class.getSimpleName()));
                }
                throw new NoSuchBufferException(String.format("the pipeline does not contain a %s whose outbound buffer is %s.", ChannelOutboundHandler.class.getSimpleName(), ByteBuf.class.getSimpleName()));
            }
            if (ctx.hasOutboundByteBuffer()) {
                if (ctx.executor().inEventLoop(currentThread)) {
                    return ctx.outboundByteBuffer();
                }
                DefaultChannelHandlerContext.ByteBridge bridge = ctx.outByteBridge.get();
                if (bridge == null && !ctx.outByteBridge.compareAndSet((DefaultChannelHandlerContext.ByteBridge)null, bridge = new DefaultChannelHandlerContext.ByteBridge(ctx))) {
                    bridge = ctx.outByteBridge.get();
                }
                return bridge.byteBuf;
            }
            ctx = ctx.prev;
        } while (true);
    }

    MessageBuf<Object> nextOutboundMessageBuffer(DefaultChannelHandlerContext ctx) {
        DefaultChannelHandlerContext initialCtx = ctx;
        Thread currentThread = Thread.currentThread();
        do {
            if (ctx == null) {
                if (initialCtx.next != null) {
                    throw new NoSuchBufferException(String.format("the handler '%s' could not find a %s whose outbound buffer is %s.", initialCtx.next.name(), ChannelOutboundHandler.class.getSimpleName(), MessageBuf.class.getSimpleName()));
                }
                throw new NoSuchBufferException(String.format("the pipeline does not contain a %s whose outbound buffer is %s.", ChannelOutboundHandler.class.getSimpleName(), MessageBuf.class.getSimpleName()));
            }
            if (ctx.hasOutboundMessageBuffer()) {
                if (ctx.executor().inEventLoop(currentThread)) {
                    return ctx.outboundMessageBuffer();
                }
                DefaultChannelHandlerContext.MessageBridge bridge = ctx.outMsgBridge.get();
                if (bridge == null && !ctx.outMsgBridge.compareAndSet((DefaultChannelHandlerContext.MessageBridge)null, bridge = new DefaultChannelHandlerContext.MessageBridge())) {
                    bridge = ctx.outMsgBridge.get();
                }
                return bridge.msgBuf;
            }
            ctx = ctx.prev;
        } while (true);
    }

    @Override
    public void fireChannelRegistered() {
        this.head.fireChannelRegistered();
    }

    @Override
    public void fireChannelUnregistered() {
        this.head.fireChannelUnregistered();
        if (!this.channel.isOpen()) {
            this.head.callFreeInboundBuffer();
        }
    }

    @Override
    public void fireChannelActive() {
        this.firedChannelActive = true;
        this.head.fireChannelActive();
        if (this.fireInboundBufferUpdatedOnActivation) {
            this.fireInboundBufferUpdatedOnActivation = false;
            this.head.fireInboundBufferUpdated();
        }
    }

    @Override
    public void fireChannelInactive() {
        this.head.fireChannelInactive();
    }

    @Override
    public void fireExceptionCaught(Throwable cause) {
        this.head.fireExceptionCaught(cause);
    }

    @Override
    public void fireUserEventTriggered(Object event) {
        this.head.fireUserEventTriggered(event);
    }

    @Override
    public void fireInboundBufferUpdated() {
        if (!this.firedChannelActive) {
            this.fireInboundBufferUpdatedOnActivation = true;
            return;
        }
        this.head.fireInboundBufferUpdated();
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        return this.bind(localAddress, this.channel.newFuture());
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress) {
        return this.connect(remoteAddress, this.channel.newFuture());
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
        return this.connect(remoteAddress, localAddress, this.channel.newFuture());
    }

    @Override
    public ChannelFuture disconnect() {
        return this.disconnect(this.channel.newFuture());
    }

    @Override
    public ChannelFuture close() {
        return this.close(this.channel.newFuture());
    }

    @Override
    public ChannelFuture deregister() {
        return this.deregister(this.channel.newFuture());
    }

    @Override
    public ChannelFuture flush() {
        return this.flush(this.channel.newFuture());
    }

    @Override
    public ChannelFuture write(Object message) {
        if (message instanceof FileRegion) {
            return this.sendFile((FileRegion)message);
        }
        return this.write(message, this.channel.newFuture());
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress, ChannelFuture future) {
        return this.bind(this.firstContext(2), localAddress, future);
    }

    ChannelFuture bind(final DefaultChannelHandlerContext ctx, final SocketAddress localAddress, final ChannelFuture future) {
        if (localAddress == null) {
            throw new NullPointerException("localAddress");
        }
        this.validateFuture(future);
        EventExecutor executor = ctx.executor();
        if (executor.inEventLoop()) {
            try {
                ((ChannelOperationHandler)ctx.handler()).bind(ctx, localAddress, future);
            }
            catch (Throwable t) {
                this.notifyHandlerException(t);
            }
        } else {
            executor.execute(new Runnable(){

                @Override
                public void run() {
                    DefaultChannelPipeline.this.bind(ctx, localAddress, future);
                }
            });
        }
        return future;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, ChannelFuture future) {
        return this.connect(remoteAddress, null, future);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelFuture future) {
        return this.connect(this.firstContext(2), remoteAddress, localAddress, future);
    }

    ChannelFuture connect(final DefaultChannelHandlerContext ctx, final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelFuture future) {
        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress");
        }
        this.validateFuture(future);
        EventExecutor executor = ctx.executor();
        if (executor.inEventLoop()) {
            try {
                ((ChannelOperationHandler)ctx.handler()).connect(ctx, remoteAddress, localAddress, future);
            }
            catch (Throwable t) {
                this.notifyHandlerException(t);
            }
        } else {
            executor.execute(new Runnable(){

                @Override
                public void run() {
                    DefaultChannelPipeline.this.connect(ctx, remoteAddress, localAddress, future);
                }
            });
        }
        return future;
    }

    @Override
    public ChannelFuture disconnect(ChannelFuture future) {
        return this.disconnect(this.firstContext(2), future);
    }

    ChannelFuture disconnect(final DefaultChannelHandlerContext ctx, final ChannelFuture future) {
        if (!ctx.channel().metadata().hasDisconnect()) {
            return this.close(ctx, future);
        }
        this.validateFuture(future);
        EventExecutor executor = ctx.executor();
        if (executor.inEventLoop()) {
            try {
                ((ChannelOperationHandler)ctx.handler()).disconnect(ctx, future);
            }
            catch (Throwable t) {
                this.notifyHandlerException(t);
            }
        } else {
            executor.execute(new Runnable(){

                @Override
                public void run() {
                    DefaultChannelPipeline.this.disconnect(ctx, future);
                }
            });
        }
        return future;
    }

    @Override
    public ChannelFuture close(ChannelFuture future) {
        return this.close(this.firstContext(2), future);
    }

    ChannelFuture close(final DefaultChannelHandlerContext ctx, final ChannelFuture future) {
        this.validateFuture(future);
        EventExecutor executor = ctx.executor();
        if (executor.inEventLoop()) {
            try {
                ((ChannelOperationHandler)ctx.handler()).close(ctx, future);
            }
            catch (Throwable t) {
                this.notifyHandlerException(t);
            }
        } else {
            executor.execute(new Runnable(){

                @Override
                public void run() {
                    DefaultChannelPipeline.this.close(ctx, future);
                }
            });
        }
        return future;
    }

    @Override
    public ChannelFuture deregister(ChannelFuture future) {
        return this.deregister(this.firstContext(2), future);
    }

    ChannelFuture deregister(final DefaultChannelHandlerContext ctx, final ChannelFuture future) {
        this.validateFuture(future);
        EventExecutor executor = ctx.executor();
        if (executor.inEventLoop()) {
            try {
                ((ChannelOperationHandler)ctx.handler()).deregister(ctx, future);
            }
            catch (Throwable t) {
                this.notifyHandlerException(t);
            }
        } else {
            executor.execute(new Runnable(){

                @Override
                public void run() {
                    DefaultChannelPipeline.this.deregister(ctx, future);
                }
            });
        }
        return future;
    }

    @Override
    public ChannelFuture sendFile(FileRegion region) {
        return this.sendFile(region, this.channel().newFuture());
    }

    @Override
    public ChannelFuture sendFile(FileRegion region, ChannelFuture future) {
        return this.sendFile(this.firstContext(2), region, future);
    }

    ChannelFuture sendFile(final DefaultChannelHandlerContext ctx, final FileRegion region, final ChannelFuture future) {
        this.validateFuture(future);
        EventExecutor executor = ctx.executor();
        if (executor.inEventLoop()) {
            try {
                ctx.flushBridge();
                ((ChannelOperationHandler)ctx.handler()).sendFile(ctx, region, future);
            }
            catch (Throwable t) {
                this.notifyHandlerException(t);
            }
        } else {
            executor.execute(new Runnable(){

                @Override
                public void run() {
                    DefaultChannelPipeline.this.sendFile(ctx, region, future);
                }
            });
        }
        return future;
    }

    @Override
    public ChannelFuture flush(ChannelFuture future) {
        return this.flush(this.firstContext(2), future);
    }

    ChannelFuture flush(final DefaultChannelHandlerContext ctx, final ChannelFuture future) {
        this.validateFuture(future);
        EventExecutor executor = ctx.executor();
        if (executor.inEventLoop()) {
            this.flush0(ctx, future);
        } else {
            executor.execute(new Runnable(){

                @Override
                public void run() {
                    DefaultChannelPipeline.this.flush(ctx, future);
                }
            });
        }
        return future;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void flush0(DefaultChannelHandlerContext ctx, ChannelFuture future) {
        try {
            ctx.flushBridge();
            ((ChannelOperationHandler)ctx.handler()).flush(ctx, future);
        }
        catch (Throwable t) {
            this.notifyHandlerException(t);
        }
        finally {
            ByteBuf buf;
            if (ctx.hasOutboundByteBuffer() && !(buf = ctx.outboundByteBuffer()).readable()) {
                buf.discardReadBytes();
            }
        }
    }

    @Override
    public ChannelFuture write(Object message, ChannelFuture future) {
        if (message instanceof FileRegion) {
            return this.sendFile((FileRegion)message, future);
        }
        return this.write(this.tail, message, future);
    }

    ChannelFuture write(DefaultChannelHandlerContext ctx, final Object message, final ChannelFuture future) {
        EventExecutor executor;
        if (message == null) {
            throw new NullPointerException("message");
        }
        this.validateFuture(future);
        DefaultChannelHandlerContext initialCtx = ctx;
        boolean msgBuf = false;
        do {
            if (ctx == null) {
                if (initialCtx.next != null) {
                    throw new NoSuchBufferException(String.format("the handler '%s' could not find a %s which accepts a %s, and the transport does not accept it as-is.", initialCtx.next.name(), ChannelOutboundHandler.class.getSimpleName(), message.getClass().getSimpleName()));
                }
                throw new NoSuchBufferException(String.format("the pipeline does not contain a %s which accepts a %s, and the transport does not accept it as-is.", ChannelOutboundHandler.class.getSimpleName(), message.getClass().getSimpleName()));
            }
            if (ctx.hasOutboundMessageBuffer()) {
                msgBuf = true;
                executor = ctx.executor();
                break;
            }
            if (message instanceof ByteBuf && ctx.hasOutboundByteBuffer()) {
                executor = ctx.executor();
                break;
            }
            ctx = ctx.prev;
        } while (true);
        if (executor.inEventLoop()) {
            if (msgBuf) {
                ctx.outboundMessageBuffer().add((Object)message);
            } else {
                ByteBuf buf = (ByteBuf)message;
                ctx.outboundByteBuffer().writeBytes(buf, buf.readerIndex(), buf.readableBytes());
            }
            this.flush0(ctx, future);
            return future;
        }
        final DefaultChannelHandlerContext ctx0 = ctx;
        executor.execute(new Runnable(){

            @Override
            public void run() {
                DefaultChannelPipeline.this.write(ctx0, message, future);
            }
        });
        return future;
    }

    private void validateFuture(ChannelFuture future) {
        if (future == null) {
            throw new NullPointerException("future");
        }
        if (future.channel() != this.channel) {
            throw new IllegalArgumentException(String.format("future.channel does not match: %s (expected: %s)", future.channel(), this.channel));
        }
        if (future.isDone()) {
            throw new IllegalArgumentException("future already done");
        }
        if (future instanceof ChannelFuture.Unsafe) {
            throw new IllegalArgumentException("internal use only future not allowed");
        }
    }

    DefaultChannelHandlerContext firstContext(int direction) {
        assert (direction == 1 || direction == 2);
        if (direction == 1) {
            return DefaultChannelPipeline.nextContext(this.head.next, 1);
        }
        return DefaultChannelPipeline.nextContext(this.tail, 2);
    }

    static DefaultChannelHandlerContext nextContext(DefaultChannelHandlerContext ctx, int direction) {
        assert (direction == 1 || direction == 2);
        if (ctx == null) {
            return null;
        }
        DefaultChannelHandlerContext realCtx = ctx;
        if (direction == 1) {
            while ((realCtx.flags & 1) == 0) {
                realCtx = realCtx.next;
                if (realCtx != null) continue;
                return null;
            }
        } else {
            while ((realCtx.flags & 2) == 0) {
                realCtx = realCtx.prev;
                if (realCtx != null) continue;
                return null;
            }
        }
        return realCtx;
    }

    protected void notifyHandlerException(Throwable cause) {
        if (!(cause instanceof ChannelPipelineException)) {
            cause = new ChannelPipelineException(cause);
        }
        if (DefaultChannelPipeline.inExceptionCaught(cause)) {
            if (logger.isWarnEnabled()) {
                logger.warn("An exception was thrown by a user handler while handling an exceptionCaught event", cause);
            }
            return;
        }
        this.fireExceptionCaught(cause);
    }

    private static boolean inExceptionCaught(Throwable cause) {
        while (cause != null) {
            StackTraceElement[] trace = cause.getStackTrace();
            if (trace != null) {
                for (StackTraceElement t : trace) {
                    if (!"exceptionCaught".equals(t.getMethodName())) continue;
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }

    private void checkDuplicateName(String name) {
        if (this.name2ctx.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate handler name: " + name);
        }
    }

    private DefaultChannelHandlerContext getContextOrDie(String name) {
        DefaultChannelHandlerContext ctx = (DefaultChannelHandlerContext)this.context(name);
        if (ctx == null || ctx == this.head) {
            throw new NoSuchElementException(name);
        }
        return ctx;
    }

    private DefaultChannelHandlerContext getContextOrDie(ChannelHandler handler) {
        DefaultChannelHandlerContext ctx = (DefaultChannelHandlerContext)this.context(handler);
        if (ctx == null || ctx == this.head) {
            throw new NoSuchElementException(handler.getClass().getName());
        }
        return ctx;
    }

    private DefaultChannelHandlerContext getContextOrDie(Class<? extends ChannelHandler> handlerType) {
        DefaultChannelHandlerContext ctx = (DefaultChannelHandlerContext)this.context(handlerType);
        if (ctx == null || ctx == this.head) {
            throw new NoSuchElementException(handlerType.getName());
        }
        return ctx;
    }

    void readable(DefaultChannelHandlerContext ctx, boolean readable) {
        if (ctx.readable.compareAndSet(!readable, readable)) {
            if (!readable) {
                if (this.suspendRead.incrementAndGet() == 1) {
                    this.unsafe.suspendRead();
                }
            } else if (this.suspendRead.decrementAndGet() == 0) {
                this.unsafe.resumeRead();
            }
        }
    }

    private final class HeadHandler
    implements ChannelOutboundHandler {
        private HeadHandler() {
        }

        @Override
        public ChannelBuf newOutboundBuffer(ChannelHandlerContext ctx) throws Exception {
            switch (DefaultChannelPipeline.this.channel.metadata().bufferType()) {
                case BYTE: {
                    return ctx.alloc().ioBuffer();
                }
                case MESSAGE: {
                    return Unpooled.messageBuffer();
                }
            }
            throw new Error();
        }

        @Override
        public void freeOutboundBuffer(ChannelHandlerContext ctx, ChannelBuf buf) {
            buf.unsafe().free();
        }

        @Override
        public void beforeAdd(ChannelHandlerContext ctx) throws Exception {
        }

        @Override
        public void afterAdd(ChannelHandlerContext ctx) throws Exception {
        }

        @Override
        public void beforeRemove(ChannelHandlerContext ctx) throws Exception {
        }

        @Override
        public void afterRemove(ChannelHandlerContext ctx) throws Exception {
        }

        @Override
        public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelFuture future) throws Exception {
            DefaultChannelPipeline.this.unsafe.bind(localAddress, future);
        }

        @Override
        public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelFuture future) throws Exception {
            DefaultChannelPipeline.this.unsafe.connect(remoteAddress, localAddress, future);
        }

        @Override
        public void disconnect(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
            DefaultChannelPipeline.this.unsafe.disconnect(future);
        }

        @Override
        public void close(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
            DefaultChannelPipeline.this.unsafe.close(future);
        }

        @Override
        public void deregister(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
            DefaultChannelPipeline.this.unsafe.deregister(future);
        }

        @Override
        public void flush(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
            DefaultChannelPipeline.this.unsafe.flush(future);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.fireExceptionCaught(cause);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void sendFile(ChannelHandlerContext ctx, FileRegion region, ChannelFuture future) throws Exception {
            DefaultChannelPipeline.this.unsafe.sendFile(region, future);
        }
    }

}

