
package io.netty.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import java.util.EventListener;

public interface ChannelFutureListener
extends EventListener {
    public static final ChannelFutureListener CLOSE = new ChannelFutureListener(){

        @Override
        public void operationComplete(ChannelFuture future) {
            future.channel().close();
        }
    };
    public static final ChannelFutureListener CLOSE_ON_FAILURE = new ChannelFutureListener(){

        @Override
        public void operationComplete(ChannelFuture future) {
            if (!future.isSuccess()) {
                future.channel().close();
            }
        }
    };

    public void operationComplete(ChannelFuture var1) throws Exception;

}

