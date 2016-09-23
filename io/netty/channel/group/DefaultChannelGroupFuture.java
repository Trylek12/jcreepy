
package io.netty.channel.group;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.ChannelGroupFutureListener;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DefaultChannelGroupFuture
implements ChannelGroupFuture {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultChannelGroupFuture.class);
    private final ChannelGroup group;
    final Map<Integer, ChannelFuture> futures;
    private ChannelGroupFutureListener firstListener;
    private List<ChannelGroupFutureListener> otherListeners;
    private boolean done;
    int successCount;
    int failureCount;
    private int waiters;
    private final ChannelFutureListener childListener;

    public DefaultChannelGroupFuture(ChannelGroup group, Collection<ChannelFuture> futures) {
        this.childListener = new ChannelFutureListener(){

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                boolean callSetDone;
                boolean success = future.isSuccess();
                DefaultChannelGroupFuture defaultChannelGroupFuture = DefaultChannelGroupFuture.this;
                synchronized (defaultChannelGroupFuture) {
                    if (success) {
                        ++DefaultChannelGroupFuture.this.successCount;
                    } else {
                        ++DefaultChannelGroupFuture.this.failureCount;
                    }
                    boolean bl = callSetDone = DefaultChannelGroupFuture.this.successCount + DefaultChannelGroupFuture.this.failureCount == DefaultChannelGroupFuture.this.futures.size();
                    assert (DefaultChannelGroupFuture.this.successCount + DefaultChannelGroupFuture.this.failureCount <= DefaultChannelGroupFuture.this.futures.size());
                }
                if (callSetDone) {
                    DefaultChannelGroupFuture.this.setDone();
                }
            }
        };
        if (group == null) {
            throw new NullPointerException("group");
        }
        if (futures == null) {
            throw new NullPointerException("futures");
        }
        this.group = group;
        LinkedHashMap<Integer, ChannelFuture> futureMap = new LinkedHashMap<Integer, ChannelFuture>();
        for (ChannelFuture f2 : futures) {
            futureMap.put(f2.channel().id(), f2);
        }
        this.futures = Collections.unmodifiableMap(futureMap);
        for (ChannelFuture f2 : this.futures.values()) {
            f2.addListener(this.childListener);
        }
        if (this.futures.isEmpty()) {
            this.setDone();
        }
    }

    DefaultChannelGroupFuture(ChannelGroup group, Map<Integer, ChannelFuture> futures) {
        this.childListener = new ;
        this.group = group;
        this.futures = Collections.unmodifiableMap(futures);
        for (ChannelFuture f : this.futures.values()) {
            f.addListener(this.childListener);
        }
        if (this.futures.isEmpty()) {
            this.setDone();
        }
    }

    @Override
    public ChannelGroup getGroup() {
        return this.group;
    }

    @Override
    public ChannelFuture find(Integer channelId) {
        return this.futures.get(channelId);
    }

    @Override
    public ChannelFuture find(Channel channel) {
        return this.futures.get(channel.id());
    }

    @Override
    public Iterator<ChannelFuture> iterator() {
        return this.futures.values().iterator();
    }

    @Override
    public synchronized boolean isDone() {
        return this.done;
    }

    @Override
    public synchronized boolean isCompleteSuccess() {
        return this.successCount == this.futures.size();
    }

    @Override
    public synchronized boolean isPartialSuccess() {
        return this.successCount != 0 && this.successCount != this.futures.size();
    }

    @Override
    public synchronized boolean isPartialFailure() {
        return this.failureCount != 0 && this.failureCount != this.futures.size();
    }

    @Override
    public synchronized boolean isCompleteFailure() {
        int futureCnt = this.futures.size();
        return futureCnt != 0 && this.failureCount == futureCnt;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void addListener(ChannelGroupFutureListener listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        boolean notifyNow = false;
        DefaultChannelGroupFuture defaultChannelGroupFuture = this;
        synchronized (defaultChannelGroupFuture) {
            if (this.done) {
                notifyNow = true;
            } else if (this.firstListener == null) {
                this.firstListener = listener;
            } else {
                if (this.otherListeners == null) {
                    this.otherListeners = new ArrayList<ChannelGroupFutureListener>(1);
                }
                this.otherListeners.add(listener);
            }
        }
        if (notifyNow) {
            this.notifyListener(listener);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void removeListener(ChannelGroupFutureListener listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        DefaultChannelGroupFuture defaultChannelGroupFuture = this;
        synchronized (defaultChannelGroupFuture) {
            if (!this.done) {
                if (listener == this.firstListener) {
                    this.firstListener = this.otherListeners != null && !this.otherListeners.isEmpty() ? this.otherListeners.remove(0) : null;
                } else if (this.otherListeners != null) {
                    this.otherListeners.remove(listener);
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public ChannelGroupFuture await() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        DefaultChannelGroupFuture defaultChannelGroupFuture = this;
        synchronized (defaultChannelGroupFuture) {
            while (!this.done) {
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
    public ChannelGroupFuture awaitUninterruptibly() {
        boolean interrupted = false;
        DefaultChannelGroupFuture defaultChannelGroupFuture = this;
        synchronized (defaultChannelGroupFuture) {
            while (!this.done) {
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

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void setDone() {
        DefaultChannelGroupFuture defaultChannelGroupFuture = this;
        synchronized (defaultChannelGroupFuture) {
            if (this.done) {
                return;
            }
            this.done = true;
            if (this.waiters > 0) {
                this.notifyAll();
            }
        }
        this.notifyListeners();
    }

    private void notifyListeners() {
        if (this.firstListener != null) {
            this.notifyListener(this.firstListener);
            this.firstListener = null;
            if (this.otherListeners != null) {
                for (ChannelGroupFutureListener l : this.otherListeners) {
                    this.notifyListener(l);
                }
                this.otherListeners = null;
            }
        }
    }

    private void notifyListener(ChannelGroupFutureListener l) {
        block2 : {
            try {
                l.operationComplete(this);
            }
            catch (Throwable t) {
                if (!logger.isWarnEnabled()) break block2;
                logger.warn("An exception was thrown by " + ChannelFutureListener.class.getSimpleName() + '.', t);
            }
        }
    }

}

