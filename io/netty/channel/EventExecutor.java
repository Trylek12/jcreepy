
package io.netty.channel;

import io.netty.channel.EventExecutorGroup;
import java.util.concurrent.ScheduledExecutorService;

public interface EventExecutor
extends EventExecutorGroup,
ScheduledExecutorService {
    @Override
    public EventExecutor next();

    public EventExecutorGroup parent();

    public boolean inEventLoop();

    public boolean inEventLoop(Thread var1);
}

