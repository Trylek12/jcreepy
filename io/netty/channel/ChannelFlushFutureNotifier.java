
package io.netty.channel;

import io.netty.channel.ChannelFuture;
import java.util.ArrayDeque;
import java.util.Queue;

public class ChannelFlushFutureNotifier {
    private long writeCounter;
    private final Queue<FlushCheckpoint> flushCheckpoints = new ArrayDeque<FlushCheckpoint>();

    public void addFlushFuture(ChannelFuture future, int pendingDataSize) {
        long checkpoint = this.writeCounter + (long)pendingDataSize;
        if (future instanceof FlushCheckpoint) {
            FlushCheckpoint cp = (FlushCheckpoint)((Object)future);
            cp.flushCheckpoint(checkpoint);
            this.flushCheckpoints.add(cp);
        } else {
            this.flushCheckpoints.add(new DefaultFlushCheckpoint(checkpoint, future));
        }
    }

    public void increaseWriteCounter(long delta) {
        this.writeCounter += delta;
    }

    public void notifyFlushFutures() {
        this.notifyFlushFutures0(null);
    }

    public void notifyFlushFutures(Throwable cause) {
        FlushCheckpoint cp;
        this.notifyFlushFutures();
        while ((cp = this.flushCheckpoints.poll()) != null) {
            cp.future().setFailure(cause);
        }
    }

    public void notifyFlushFutures(Throwable cause1, Throwable cause2) {
        FlushCheckpoint cp;
        this.notifyFlushFutures0(cause1);
        while ((cp = this.flushCheckpoints.poll()) != null) {
            cp.future().setFailure(cause2);
        }
    }

    private void notifyFlushFutures0(Throwable cause) {
        if (this.flushCheckpoints.isEmpty()) {
            this.writeCounter = 0;
            return;
        }
        long writeCounter = this.writeCounter;
        do {
            FlushCheckpoint cp;
            if ((cp = this.flushCheckpoints.peek()) == null) {
                this.writeCounter = 0;
                break;
            }
            if (cp.flushCheckpoint() > writeCounter) {
                if (writeCounter <= 0 || this.flushCheckpoints.size() != 1) break;
                this.writeCounter = 0;
                cp.flushCheckpoint(cp.flushCheckpoint() - writeCounter);
                break;
            }
            this.flushCheckpoints.remove();
            if (cause == null) {
                cp.future().setSuccess();
                continue;
            }
            cp.future().setFailure(cause);
        } while (true);
        long newWriteCounter = this.writeCounter;
        if (newWriteCounter >= 0x1000000000000000L) {
            this.writeCounter = 0;
            for (FlushCheckpoint cp : this.flushCheckpoints) {
                cp.flushCheckpoint(cp.flushCheckpoint() - newWriteCounter);
            }
        }
    }

    private static class DefaultFlushCheckpoint
    extends FlushCheckpoint {
        private long checkpoint;
        private final ChannelFuture future;

        DefaultFlushCheckpoint(long checkpoint, ChannelFuture future) {
            this.checkpoint = checkpoint;
            this.future = future;
        }

        @Override
        long flushCheckpoint() {
            return this.checkpoint;
        }

        @Override
        void flushCheckpoint(long checkpoint) {
            this.checkpoint = checkpoint;
        }

        @Override
        ChannelFuture future() {
            return this.future;
        }
    }

    static abstract class FlushCheckpoint {
        FlushCheckpoint() {
        }

        abstract long flushCheckpoint();

        abstract void flushCheckpoint(long var1);

        abstract ChannelFuture future();
    }

}

