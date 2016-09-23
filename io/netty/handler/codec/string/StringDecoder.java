
package io.netty.handler.codec.string;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.nio.charset.Charset;

@ChannelHandler.Sharable
public class StringDecoder
extends MessageToMessageDecoder<ByteBuf, String> {
    private final Charset charset;

    public StringDecoder() {
        this(Charset.defaultCharset());
    }

    public StringDecoder(Charset charset) {
        super(ByteBuf.class);
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        this.charset = charset;
    }

    @Override
    public String decode(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        return msg.toString(this.charset);
    }
}

