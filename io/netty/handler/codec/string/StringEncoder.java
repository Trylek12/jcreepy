
package io.netty.handler.codec.string;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.nio.charset.Charset;

@ChannelHandler.Sharable
public class StringEncoder
extends MessageToMessageEncoder<CharSequence, ByteBuf> {
    private final Charset charset;

    public StringEncoder() {
        this(Charset.defaultCharset());
    }

    public StringEncoder(Charset charset) {
        super(CharSequence.class);
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        this.charset = charset;
    }

    @Override
    public ByteBuf encode(ChannelHandlerContext ctx, CharSequence msg) throws Exception {
        return Unpooled.copiedBuffer(msg, this.charset);
    }
}

