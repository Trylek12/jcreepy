
package jcreepy.network;

import io.netty.buffer.ByteBuf;
import jcreepy.network.Packet;
import jcreepy.network.codec.CodecLookupService;
import jcreepy.network.codec.PacketCodec;
import jcreepy.network.exception.UnknownPacketException;

public interface Protocol {
    public PacketCodec<?> readHeader(ByteBuf var1) throws UnknownPacketException;

    public ByteBuf writeHeader(PacketCodec<?> var1, ByteBuf var2);

    public CodecLookupService getCodecLookupService();

    public Packet getHandshakePacket(String var1);

    public int getVersion();
}

