
package io.netty.channel;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

public final class ChannelFutureNotifier
implements ChannelFutureListener {
    private final ChannelFuture future;

    public ChannelFutureNotifier(ChannelFuture future) {
        this.future = future;
    }

    @Override
    public void operationComplete(ChannelFuture cf) throws Exception {
        if (cf.isSuccess()) {
            this.future.setSuccess();
        } else {
            this.future.setFailure(cf.cause());
        }
    }
}

