
package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.MessageBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandlerUtil;
import io.netty.channel.ChannelOutboundMessageHandlerAdapter;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.EncoderException;

public abstract class MessageToByteEncoder<I>
extends ChannelOutboundMessageHandlerAdapter<I> {
    private final Class<?>[] acceptedMsgTypes;

    protected /* varargs */ MessageToByteEncoder(Class<?> ... acceptedMsgTypes) {
        this.acceptedMsgTypes = ChannelHandlerUtil.acceptedMessageTypes(acceptedMsgTypes);
    }

    @Override
    public void flush(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
        Object msg;
        MessageBuf in = ctx.outboundMessageBuffer();
        ByteBuf out = ctx.nextOutboundByteBuffer();
        while ((msg = in.poll()) != null) {
            if (!this.isEncodable(msg)) {
                ChannelHandlerUtil.addToNextOutboundBuffer(ctx, msg);
                continue;
            }
            Object imsg = msg;
            try {
                this.encode(ctx, imsg, out);
            }
            catch (Throwable t) {
                if (t instanceof CodecException) {
                    ctx.fireExceptionCaught(t);
                    continue;
                }
                ctx.fireExceptionCaught(new EncoderException(t));
            }
        }
        ctx.flush(future);
    }

    public boolean isEncodable(Object msg) throws Exception {
        return ChannelHandlerUtil.acceptMessage(this.acceptedMsgTypes, msg);
    }

    public abstract void encode(ChannelHandlerContext var1, I var2, ByteBuf var3) throws Exception;
}

