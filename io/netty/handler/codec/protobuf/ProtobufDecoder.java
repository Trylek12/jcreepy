/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  com.google.protobuf.ExtensionRegistry
 *  com.google.protobuf.ExtensionRegistryLite
 *  com.google.protobuf.MessageLite
 *  com.google.protobuf.MessageLite$Builder
 */
package io.netty.handler.codec.protobuf;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.MessageLite;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.io.InputStream;

@ChannelHandler.Sharable
public class ProtobufDecoder
extends MessageToMessageDecoder<ByteBuf, MessageLite> {
    private final MessageLite prototype;
    private final ExtensionRegistry extensionRegistry;

    public ProtobufDecoder(MessageLite prototype) {
        this(prototype, null);
    }

    public ProtobufDecoder(MessageLite prototype, ExtensionRegistry extensionRegistry) {
        super(ByteBuf.class);
        if (prototype == null) {
            throw new NullPointerException("prototype");
        }
        this.prototype = prototype.getDefaultInstanceForType();
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public MessageLite decode(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        if (msg.hasArray()) {
            int offset = msg.readerIndex();
            if (this.extensionRegistry == null) {
                return this.prototype.newBuilderForType().mergeFrom(msg.array(), msg.arrayOffset() + offset, msg.readableBytes()).build();
            }
            return this.prototype.newBuilderForType().mergeFrom(msg.array(), msg.arrayOffset() + offset, msg.readableBytes(), (ExtensionRegistryLite)this.extensionRegistry).build();
        }
        if (this.extensionRegistry == null) {
            return this.prototype.newBuilderForType().mergeFrom((InputStream)new ByteBufInputStream(msg)).build();
        }
        return this.prototype.newBuilderForType().mergeFrom((InputStream)new ByteBufInputStream(msg), (ExtensionRegistryLite)this.extensionRegistry).build();
    }
}

