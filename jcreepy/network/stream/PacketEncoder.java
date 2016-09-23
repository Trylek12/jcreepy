
package jcreepy.network.stream;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import jcreepy.network.Packet;
import jcreepy.network.Protocol;
import jcreepy.network.codec.CodecLookupService;
import jcreepy.network.codec.PacketCodec;

public class PacketEncoder
extends MessageToByteEncoder<Packet> {
    private Protocol protocol;

    public PacketEncoder(Protocol protocol) {
        super(new Class[0]);
        this.protocol = protocol;
    }

    @Override
    public void encode(ChannelHandlerContext channelHandlerContext, Packet msg, ByteBuf out) throws Exception {
        PacketCodec codec = this.protocol.getCodecLookupService().find(msg.getClass());
        if (codec != null) {
            ByteBuf data = codec.encode((Packet)msg);
            ByteBuf header = this.protocol.writeHeader(codec, data);
            out.writeBytes(header);
            out.writeBytes(data);
        }
    }
}

