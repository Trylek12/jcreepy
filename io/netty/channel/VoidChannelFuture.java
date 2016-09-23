
package io.netty.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class VoidChannelFuture
implements ChannelFuture.Unsafe {
    private final Channel channel;

    public VoidChannelFuture(Channel channel) {
        if (channel == null) {
            throw new NullPointerException("channel");
        }
        this.channel = channel;
    }

    @Override
    public ChannelFuture addListener(ChannelFutureListener listener) {
        VoidChannelFuture.fail();
        return this;
    }

    @Override
    public ChannelFuture removeListener(ChannelFutureListener listener) {
        return this;
    }

    @Override
    public ChannelFuture await() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        return this;
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        VoidChannelFuture.fail();
        return false;
    }

    @Override
    public boolean await(long timeoutMillis) throws InterruptedException {
        VoidChannelFuture.fail();
        return false;
    }

    @Override
    public ChannelFuture awaitUninterruptibly() {
        VoidChannelFuture.fail();
        return this;
    }

    @Override
    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        VoidChannelFuture.fail();
        return false;
    }

    @Override
    public boolean awaitUninterruptibly(long timeoutMillis) {
        VoidChannelFuture.fail();
        return false;
    }

    @Override
    public Channel channel() {
        return this.channel;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public Throwable cause() {
        return null;
    }

    @Override
    public ChannelFuture sync() throws InterruptedException {
        VoidChannelFuture.fail();
        return this;
    }

    @Override
    public ChannelFuture syncUninterruptibly() {
        VoidChannelFuture.fail();
        return this;
    }

    @Override
    public Void get() throws InterruptedException, ExecutionException {
        VoidChannelFuture.fail();
        return null;
    }

    @Override
    public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        VoidChannelFuture.fail();
        return null;
    }

    @Override
    public boolean setProgress(long amount, long current, long total) {
        return false;
    }

    @Override
    public boolean setFailure(Throwable cause) {
        return false;
    }

    @Override
    public boolean setSuccess() {
        return false;
    }

    @Override
    public boolean cancel() {
        return false;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    private static void fail() {
        throw new IllegalStateException("void future");
    }
}

