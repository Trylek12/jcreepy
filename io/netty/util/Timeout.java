
package io.netty.util;

import io.netty.util.Timer;
import io.netty.util.TimerTask;

public interface Timeout {
    public Timer getTimer();

    public TimerTask getTask();

    public boolean isExpired();

    public boolean isCancelled();

    public boolean cancel();
}

