
package io.netty.channel;

import io.netty.channel.EventExecutor;
import java.util.concurrent.TimeUnit;

public interface EventExecutorGroup {
    public EventExecutor next();

    public void shutdown();

    public boolean isShutdown();

    public boolean isTerminated();

    public boolean awaitTermination(long var1, TimeUnit var3) throws InterruptedException;
}

