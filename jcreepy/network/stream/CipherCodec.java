
package jcreepy.network.stream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToByteCodec;
import javax.crypto.Cipher;

public class CipherCodec
extends ByteToByteCodec {
    private final Cipher encrypt;
    private final Cipher decrypt;
    private final ByteBuf heapOut = Unpooled.buffer();

    public CipherCodec(Cipher encrypt, Cipher decrypt) {
        this.encrypt = encrypt;
        this.decrypt = decrypt;
    }

    @Override
    public void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
        if (out.hasArray()) {
            this.cipher(this.encrypt, in, out);
        } else {
            this.cipher(this.encrypt, in, this.heapOut);
            out.writeBytes(this.heapOut);
            this.heapOut.discardReadBytes();
        }
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
        this.cipher(this.decrypt, in, out);
    }

    private void cipher(Cipher cipher, ByteBuf in, ByteBuf out) throws Exception {
        int readable = in.readableBytes();
        out.capacity(out.capacity() + cipher.getOutputSize(readable));
        int processed = cipher.update(in.array(), in.arrayOffset() + in.readerIndex(), readable, out.array(), out.arrayOffset() + out.writerIndex());
        in.readerIndex(in.readerIndex() + processed);
        out.writerIndex(out.writerIndex() + processed);
    }
}

