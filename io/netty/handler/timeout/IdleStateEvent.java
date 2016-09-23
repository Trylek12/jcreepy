
package io.netty.handler.timeout;

import io.netty.handler.timeout.IdleState;

public class IdleStateEvent {
    private final IdleState state;
    private final int count;
    private final long durationMillis;

    public IdleStateEvent(IdleState state, int count, long durationMillis) {
        if (state == null) {
            throw new NullPointerException("state");
        }
        if (count < 0) {
            throw new IllegalStateException(String.format("count: %d (expected: >= 0)", count));
        }
        if (durationMillis < 0) {
            throw new IllegalStateException(String.format("durationMillis: %d (expected: >= 0)", durationMillis));
        }
        this.state = state;
        this.count = count;
        this.durationMillis = durationMillis;
    }

    public IdleState state() {
        return this.state;
    }

    public int count() {
        return this.count;
    }

    public long durationMillis() {
        return this.durationMillis;
    }

    public String toString() {
        return (Object)((Object)this.state) + "(" + this.count + ", " + this.durationMillis + "ms)";
    }
}

