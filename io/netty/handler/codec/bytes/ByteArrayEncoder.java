
package io.netty.handler.codec.bytes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ChannelBuf;
import io.netty.buffer.MessageBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

public class ByteArrayEncoder
extends MessageToMessageEncoder<byte[], ByteBuf> {
    public ByteArrayEncoder() {
        super(byte[].class);
    }

    @Override
    public MessageBuf<byte[]> newOutboundBuffer(ChannelHandlerContext ctx) throws Exception {
        return Unpooled.messageBuffer();
    }

    @Override
    public ByteBuf encode(ChannelHandlerContext ctx, byte[] msg) throws Exception {
        if (msg.length == 0) {
            return null;
        }
        return Unpooled.wrappedBuffer(msg);
    }
}

