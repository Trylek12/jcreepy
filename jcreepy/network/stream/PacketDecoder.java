
package jcreepy.network.stream;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import jcreepy.network.Packet;
import jcreepy.network.Protocol;
import jcreepy.network.codec.PacketCodec;
import jcreepy.network.exception.UnknownPacketException;

public class PacketDecoder
extends ReplayingDecoder<Packet, Void> {
    private Protocol protocol;
    private boolean streamOutOfSync;

    public PacketDecoder(Protocol protocol) {
        this.protocol = protocol;
    }

    @Override
    public Packet decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        try {
            PacketCodec codec = this.protocol.readHeader(in);
            return this.streamOutOfSync ? null : (Packet)codec.decode(in);
        }
        catch (UnknownPacketException ex) {
            if (!this.streamOutOfSync) {
                this.streamOutOfSync = true;
                ctx.channel().close();
                throw ex;
            }
            return null;
        }
    }
}

