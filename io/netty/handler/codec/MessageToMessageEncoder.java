
package io.netty.handler.codec;

import io.netty.buffer.MessageBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandlerUtil;
import io.netty.channel.ChannelOutboundMessageHandlerAdapter;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.EncoderException;

public abstract class MessageToMessageEncoder<I, O>
extends ChannelOutboundMessageHandlerAdapter<I> {
    private final Class<?>[] acceptedMsgTypes;

    protected /* varargs */ MessageToMessageEncoder(Class<?> ... acceptedMsgTypes) {
        this.acceptedMsgTypes = ChannelHandlerUtil.acceptedMessageTypes(acceptedMsgTypes);
    }

    @Override
    public void flush(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
        MessageBuf in = ctx.outboundMessageBuffer();
        do {
            try {
                Object msg;
                while ((msg = in.poll()) != null) {
                    if (!this.isEncodable(msg)) {
                        ChannelHandlerUtil.addToNextOutboundBuffer(ctx, msg);
                        continue;
                    }
                    Object imsg = msg;
                    O omsg = this.encode(ctx, imsg);
                    if (omsg == null) continue;
                    ChannelHandlerUtil.unfoldAndAdd(ctx, omsg, false);
                }
                break;
            }
            catch (Throwable t) {
                if (t instanceof CodecException) {
                    ctx.fireExceptionCaught(t);
                    continue;
                }
                ctx.fireExceptionCaught(new EncoderException(t));
                continue;
            }
            break;
        } while (true);
        ctx.flush(future);
    }

    public boolean isEncodable(Object msg) throws Exception {
        return ChannelHandlerUtil.acceptMessage(this.acceptedMsgTypes, msg);
    }

    public abstract O encode(ChannelHandlerContext var1, I var2) throws Exception;
}

