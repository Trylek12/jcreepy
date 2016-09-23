
package io.netty.channel;

import io.netty.buffer.ChannelBuf;
import io.netty.buffer.MessageBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandlerUtil;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInboundMessageHandler;

public abstract class ChannelInboundMessageHandlerAdapter<I>
extends ChannelInboundHandlerAdapter
implements ChannelInboundMessageHandler<I> {
    private final Class<?>[] acceptedMsgTypes;

    protected /* varargs */ ChannelInboundMessageHandlerAdapter(Class<?> ... acceptedMsgTypes) {
        this.acceptedMsgTypes = ChannelHandlerUtil.acceptedMessageTypes(acceptedMsgTypes);
    }

    @Override
    public MessageBuf<I> newInboundBuffer(ChannelHandlerContext ctx) throws Exception {
        return Unpooled.messageBuffer();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public final void inboundBufferUpdated(ChannelHandlerContext ctx) throws Exception {
        if (!this.beginMessageReceived(ctx)) {
            return;
        }
        boolean unsupportedFound = false;
        try {
            Object msg;
            MessageBuf in = ctx.inboundMessageBuffer();
            while ((msg = in.poll()) != null) {
                try {
                    if (!this.isSupported(msg)) {
                        ChannelHandlerUtil.addToNextInboundBuffer(ctx, msg);
                        unsupportedFound = true;
                        continue;
                    }
                    if (unsupportedFound) {
                        unsupportedFound = false;
                        ctx.fireInboundBufferUpdated();
                    }
                    this.messageReceived(ctx, msg);
                }
                catch (Throwable t) {
                    this.exceptionCaught(ctx, t);
                }
            }
        }
        finally {
            if (unsupportedFound) {
                ctx.fireInboundBufferUpdated();
            }
        }
        this.endMessageReceived(ctx);
    }

    public boolean isSupported(Object msg) throws Exception {
        return ChannelHandlerUtil.acceptMessage(this.acceptedMsgTypes, msg);
    }

    public boolean beginMessageReceived(ChannelHandlerContext ctx) throws Exception {
        return true;
    }

    public abstract void messageReceived(ChannelHandlerContext var1, I var2) throws Exception;

    public void endMessageReceived(ChannelHandlerContext ctx) throws Exception {
    }
}

