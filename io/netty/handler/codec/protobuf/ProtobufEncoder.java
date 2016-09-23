/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  com.google.protobuf.MessageLite
 *  com.google.protobuf.MessageLite$Builder
 */
package io.netty.handler.codec.protobuf;

import com.google.protobuf.MessageLite;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

@ChannelHandler.Sharable
public class ProtobufEncoder
extends MessageToMessageEncoder<Object, ByteBuf> {
    public ProtobufEncoder() {
        super(MessageLite.class, MessageLite.Builder.class);
    }

    @Override
    public ByteBuf encode(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof MessageLite) {
            return Unpooled.wrappedBuffer(((MessageLite)msg).toByteArray());
        }
        if (msg instanceof MessageLite.Builder) {
            return Unpooled.wrappedBuffer(((MessageLite.Builder)msg).build().toByteArray());
        }
        return null;
    }
}

