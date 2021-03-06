/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  org.jboss.marshalling.ByteOutput
 *  org.jboss.marshalling.Marshaller
 */
package io.netty.handler.codec.marshalling;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.marshalling.ChannelBufferByteOutput;
import io.netty.handler.codec.marshalling.MarshallerProvider;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.Marshaller;

@ChannelHandler.Sharable
public class CompatibleMarshallingEncoder
extends MessageToByteEncoder<Object> {
    private final MarshallerProvider provider;

    public CompatibleMarshallingEncoder(MarshallerProvider provider) {
        super(new Class[0]);
        this.provider = provider;
    }

    @Override
    public void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        Marshaller marshaller = this.provider.getMarshaller(ctx);
        marshaller.start((ByteOutput)new ChannelBufferByteOutput(out));
        marshaller.writeObject(msg);
        marshaller.finish();
        marshaller.close();
    }
}

