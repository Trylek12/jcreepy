
package io.netty.handler.codec.base64;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.base64.Base64Dialect;

@ChannelHandler.Sharable
public class Base64Decoder
extends MessageToMessageDecoder<ByteBuf, ByteBuf> {
    private final Base64Dialect dialect;

    public Base64Decoder() {
        this(Base64Dialect.STANDARD);
    }

    public Base64Decoder(Base64Dialect dialect) {
        super(ByteBuf.class);
        if (dialect == null) {
            throw new NullPointerException("dialect");
        }
        this.dialect = dialect;
    }

    @Override
    public ByteBuf decode(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        return Base64.decode(msg, msg.readerIndex(), msg.readableBytes(), this.dialect);
    }
}

