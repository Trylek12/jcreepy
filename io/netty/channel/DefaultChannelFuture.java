
package io.netty.channel;

import io.netty.channel.BlockingOperationException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFlushFutureNotifier;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelFutureProgressListener;
import io.netty.channel.EventLoop;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DefaultChannelFuture
extends ChannelFlushFutureNotifier.FlushCheckpoint
implements ChannelFuture {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultChannelFuture.class);
    private static final int MAX_LISTENER_STACK_DEPTH = 8;
    private static final ThreadLocal<Integer> LISTENER_STACK_DEPTH = new ThreadLocal<Integer>(){

        @Override
        protected Integer initialValue() {
            return 0;
        }
    };
    private static final Throwable CANCELLED = new Throwable();
    private final Channel channel;
    private final boolean cancellable;
    private ChannelFutureListener firstListener;
    private List<ChannelFutureListener> otherListeners;
    private List<ChannelFutureProgressListener> progressListeners;
    private boolean done;
    private Throwable cause;
    private int waiters;
    private long flushCheckpoint;

    public DefaultChannelFuture(Channel channel, boolean cancellable) {
        this.channel = channel;
        this.cancellable = cancellable;
    }

    @Override
    public Channel channel() {
        return this.channel;
    }

    @Override
    public synchronized boolean isDone() {
        return this.done;
    }

    @Override
    public synchronized boolean isSuccess() {
        return this.done && this.cause == null;
    }

    @Override
    public synchronized Throwable cause() {
        if (this.cause != CANCELLED) {
            return this.cause;
        }
        return null;
    }

    @Override
    public synchronized boolean isCancelled() {
        return this.cause == CANCELLED;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public ChannelFuture addListener(ChannelFutureListener listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        boolean notifyNow = false;
        DefaultChannelFuture defaultChannelFuture = this;
        synchronized (defaultChannelFuture) {
            if (this.done) {
                notifyNow = true;
            } else {
                if (this.firstListener == null) {
                    this.firstListener = listener;
                } else {
                    if (this.otherListeners == null) {
                        this.otherListeners = new ArrayList<ChannelFutureListener>(1);
                    }
                    this.otherListeners.add(listener);
                }
                if (listener instanceof ChannelFutureProgressListener) {
                    if (this.progressListeners == null) {
                        this.progressListeners = new ArrayList<ChannelFutureProgressListener>(1);
                    }
                    this.progressListeners.add((ChannelFutureProgressListener)listener);
                }
            }
        }
        if (notifyNow) {
            DefaultChannelFuture.notifyListener(this, listener);
        }
        return this;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public ChannelFuture removeListener(ChannelFutureListener listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        DefaultChannelFuture defaultChannelFuture = this;
        synchronized (defaultChannelFuture) {
            if (!this.done) {
                if (listener == this.firstListener) {
                    this.firstListener = this.otherListeners != null && !this.otherListeners.isEmpty() ? this.otherListeners.remove(0) : null;
                } else if (this.otherListeners != null) {
                    this.otherListeners.remove(listener);
                }
                if (listener instanceof ChannelFutureProgressListener) {
                    this.progressListeners.remove(listener);
                }
            }
        }
        return this;
    }

    @Override
    public ChannelFuture sync() throws InterruptedException {
        this.await();
        this.rethrowIfFailed();
        return this;
    }

    @Override
    public ChannelFuture syncUninterruptibly() {
        this.awaitUninterruptibly();
        this.rethrowIfFailed();
        return this;
    }

    @Override
    public Void get() throws InterruptedException, ExecutionException {
        this.await();
        Throwable cause = this.cause();
        if (cause == null) {
            return null;
        }
        throw new ExecutionException(cause);
    }

    @Override
    public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (!this.await(timeout, unit)) {
            throw new TimeoutException();
        }
        Throwable cause = this.cause();
        if (cause == null) {
            return null;
        }
        throw new ExecutionException(cause);
    }

    private void rethrowIfFailed() {
        Throwable cause = this.cause();
        if (cause == null) {
            return;
        }
        if (cause instanceof RuntimeException) {
            throw (RuntimeException)cause;
        }
        if (cause instanceof Error) {
            throw (Error)cause;
        }
        throw new ChannelException(cause);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public ChannelFuture await() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        DefaultChannelFuture defaultChannelFuture = this;
        synchronized (defaultChannelFuture) {
            while (!this.done) {
                this.checkDeadLock();
                ++this.waiters;
                try {
                    this.wait();
                    continue;
                }
                finally {
                    --this.waiters;
                    continue;
                }
            }
        }
        return this;
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return this.await0(unit.toNanos(timeout), true);
    }

    @Override
    public boolean await(long timeoutMillis) throws InterruptedException {
        return this.await0(TimeUnit.MILLISECONDS.toNanos(timeoutMillis), true);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public ChannelFuture awaitUninterruptibly() {
        boolean interrupted = false;
        DefaultChannelFuture defaultChannelFuture = this;
        synchronized (defaultChannelFuture) {
            while (!this.done) {
                this.checkDeadLock();
                ++this.waiters;
                try {
                    this.wait();
                    continue;
                }
                catch (InterruptedException e) {
                    interrupted = true;
                    continue;
                }
                finally {
                    --this.waiters;
                    continue;
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        return this;
    }

    @Override
    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        try {
            return this.await0(unit.toNanos(timeout), false);
        }
        catch (InterruptedException e) {
            throw new InternalError();
        }
    }

    @Override
    public boolean awaitUninterruptibly(long timeoutMillis) {
        try {
            return this.await0(TimeUnit.MILLISECONDS.toNanos(timeoutMillis), false);
        }
        catch (InterruptedException e) {
            throw new InternalError();
        }
    }

    /*
     * Exception decompiling
     */
    private boolean await0(long timeoutNanos, boolean interruptable) throws InterruptedException {
        // This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
        // org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [13[DOLOOP]], but top level block is 6[TRYBLOCK]
        // org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:394)
        // org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:446)
        // org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:2869)
        // org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:817)
        // org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:220)
        // org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:165)
        // org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:91)
        // org.benf.cfr.reader.entities.Method.analyse(Method.java:354)
        // org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:751)
        // org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:683)
        // org.benf.cfr.reader.Main.doJar(Main.java:129)
        // org.benf.cfr.reader.Main.main(Main.java:181)
        throw new IllegalStateException("Decompilation failed");
    }

    private void checkDeadLock() {
        if (this.channel().isRegistered() && this.channel().eventLoop().inEventLoop()) {
            throw new BlockingOperationException();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean setSuccess() {
        DefaultChannelFuture defaultChannelFuture = this;
        synchronized (defaultChannelFuture) {
            if (this.done) {
                return false;
            }
            this.done = true;
            if (this.waiters > 0) {
                this.notifyAll();
            }
        }
        this.notifyListeners();
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean setFailure(Throwable cause) {
        DefaultChannelFuture defaultChannelFuture = this;
        synchronized (defaultChannelFuture) {
            if (this.done) {
                return false;
            }
            this.cause = cause;
            this.done = true;
            if (this.waiters > 0) {
                this.notifyAll();
            }
        }
        this.notifyListeners();
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean cancel() {
        if (!this.cancellable) {
            return false;
        }
        DefaultChannelFuture defaultChannelFuture = this;
        synchronized (defaultChannelFuture) {
            if (this.done) {
                return false;
            }
            this.cause = CANCELLED;
            this.done = true;
            if (this.waiters > 0) {
                this.notifyAll();
            }
        }
        this.notifyListeners();
        return true;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return this.cancel();
    }

    private void notifyListeners() {
        if (this.firstListener == null) {
            return;
        }
        if (this.channel().eventLoop().inEventLoop()) {
            DefaultChannelFuture.notifyListener0(this, this.firstListener);
            this.firstListener = null;
            if (this.otherListeners != null) {
                for (ChannelFutureListener l : this.otherListeners) {
                    DefaultChannelFuture.notifyListener0(this, l);
                }
                this.otherListeners = null;
            }
        } else {
            final ChannelFutureListener firstListener = this.firstListener;
            final List<ChannelFutureListener> otherListeners = this.otherListeners;
            this.firstListener = null;
            this.otherListeners = null;
            this.channel().eventLoop().execute(new Runnable(){

                @Override
                public void run() {
                    DefaultChannelFuture.notifyListener0(DefaultChannelFuture.this, firstListener);
                    if (otherListeners != null) {
                        for (ChannelFutureListener l : otherListeners) {
                            DefaultChannelFuture.notifyListener0(DefaultChannelFuture.this, l);
                        }
                    }
                }
            });
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void notifyListener(final ChannelFuture f, final ChannelFutureListener l) {
        Integer stackDepth;
        EventLoop loop = f.channel().eventLoop();
        if (loop.inEventLoop() && (stackDepth = LISTENER_STACK_DEPTH.get()) < 8) {
            LISTENER_STACK_DEPTH.set(stackDepth + 1);
            try {
                DefaultChannelFuture.notifyListener0(f, l);
            }
            finally {
                LISTENER_STACK_DEPTH.set(stackDepth);
            }
            return;
        }
        loop.execute(new Runnable(){

            @Override
            public void run() {
                DefaultChannelFuture.notifyListener(f, l);
            }
        });
    }

    private static void notifyListener0(ChannelFuture f, ChannelFutureListener l) {
        block2 : {
            try {
                l.operationComplete(f);
            }
            catch (Throwable t) {
                if (!logger.isWarnEnabled()) break block2;
                logger.warn("An exception was thrown by " + ChannelFutureListener.class.getSimpleName() + '.', t);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean setProgress(long amount, long current, long total) {
        ChannelFutureProgressListener[] plisteners;
        DefaultChannelFuture defaultChannelFuture = this;
        synchronized (defaultChannelFuture) {
            if (this.done) {
                return false;
            }
            List<ChannelFutureProgressListener> progressListeners = this.progressListeners;
            if (progressListeners == null || progressListeners.isEmpty()) {
                return true;
            }
            plisteners = progressListeners.toArray(new ChannelFutureProgressListener[progressListeners.size()]);
        }
        for (ChannelFutureProgressListener pl : plisteners) {
            this.notifyProgressListener(pl, amount, current, total);
        }
        return true;
    }

    private void notifyProgressListener(ChannelFutureProgressListener l, long amount, long current, long total) {
        block2 : {
            try {
                l.operationProgressed(this, amount, current, total);
            }
            catch (Throwable t) {
                if (!logger.isWarnEnabled()) break block2;
                logger.warn("An exception was thrown by " + ChannelFutureProgressListener.class.getSimpleName() + '.', t);
            }
        }
    }

    @Override
    long flushCheckpoint() {
        return this.flushCheckpoint;
    }

    @Override
    void flushCheckpoint(long checkpoint) {
        this.flushCheckpoint = checkpoint;
    }

    @Override
    ChannelFuture future() {
        return this;
    }

}

