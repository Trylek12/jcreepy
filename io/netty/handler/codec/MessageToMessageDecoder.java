
package io.netty.handler.codec;

import io.netty.buffer.ChannelBuf;
import io.netty.buffer.MessageBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandlerUtil;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInboundMessageHandler;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.DecoderException;

public abstract class MessageToMessageDecoder<I, O>
extends ChannelInboundHandlerAdapter
implements ChannelInboundMessageHandler<I> {
    private final Class<?>[] acceptedMsgTypes;

    protected /* varargs */ MessageToMessageDecoder(Class<?> ... acceptedMsgTypes) {
        this.acceptedMsgTypes = ChannelHandlerUtil.acceptedMessageTypes(acceptedMsgTypes);
    }

    @Override
    public MessageBuf<I> newInboundBuffer(ChannelHandlerContext ctx) throws Exception {
        return Unpooled.messageBuffer();
    }

    @Override
    public void inboundBufferUpdated(ChannelHandlerContext ctx) throws Exception {
        MessageBuf in = ctx.inboundMessageBuffer();
        boolean notify = false;
        do {
            try {
                Object msg;
                while ((msg = in.poll()) != null) {
                    if (!this.isDecodable(msg)) {
                        ChannelHandlerUtil.addToNextInboundBuffer(ctx, msg);
                        notify = true;
                        continue;
                    }
                    Object imsg = msg;
                    O omsg = this.decode(ctx, imsg);
                    if (omsg == null || !ChannelHandlerUtil.unfoldAndAdd(ctx, omsg, true)) continue;
                    notify = true;
                }
                break;
            }
            catch (Throwable t) {
                if (t instanceof CodecException) {
                    ctx.fireExceptionCaught(t);
                    continue;
                }
                ctx.fireExceptionCaught(new DecoderException(t));
                continue;
            }
            break;
        } while (true);
        if (notify) {
            ctx.fireInboundBufferUpdated();
        }
    }

    public boolean isDecodable(Object msg) throws Exception {
        return ChannelHandlerUtil.acceptMessage(this.acceptedMsgTypes, msg);
    }

    public abstract O decode(ChannelHandlerContext var1, I var2) throws Exception;
}

