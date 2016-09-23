
package io.netty.channel;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.util.HashSet;
import java.util.Set;

public class ChannelFutureAggregator
implements ChannelFutureListener {
    private final ChannelFuture aggregateFuture;
    private Set<ChannelFuture> pendingFutures;

    public ChannelFutureAggregator(ChannelFuture aggregateFuture) {
        this.aggregateFuture = aggregateFuture;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void addFuture(ChannelFuture future) {
        ChannelFutureAggregator channelFutureAggregator = this;
        synchronized (channelFutureAggregator) {
            if (this.pendingFutures == null) {
                this.pendingFutures = new HashSet<ChannelFuture>();
            }
            this.pendingFutures.add(future);
        }
        future.addListener(this);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        if (future.isCancelled()) {
            return;
        }
        ChannelFutureAggregator channelFutureAggregator = this;
        synchronized (channelFutureAggregator) {
            if (this.pendingFutures == null) {
                this.aggregateFuture.setSuccess();
            } else {
                this.pendingFutures.remove(future);
                if (!future.isSuccess()) {
                    this.aggregateFuture.setFailure(future.cause());
                    for (ChannelFuture pendingFuture : this.pendingFutures) {
                        pendingFuture.cancel();
                    }
                } else if (this.pendingFutures.isEmpty()) {
                    this.aggregateFuture.setSuccess();
                }
            }
        }
    }
}

