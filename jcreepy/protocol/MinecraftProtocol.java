
package jcreepy.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import jcreepy.network.Packet;
import jcreepy.network.Protocol;
import jcreepy.network.codec.CodecLookupService;
import jcreepy.network.codec.PacketCodec;
import jcreepy.network.exception.UnknownPacketException;
import jcreepy.protocol.codec.MinecraftCodecLookupService;
import jcreepy.protocol.packet.player.conn.PlayerHandshakePacket;

public class MinecraftProtocol
implements Protocol {
    public static final Protocol INSTANCE = new MinecraftProtocol();

    @Override
    public PacketCodec<?> readHeader(ByteBuf in) throws UnknownPacketException {
        short id = in.readUnsignedByte();
        PacketCodec codec = MinecraftCodecLookupService.INSTANCE.find(id);
        if (codec == null) {
            throw new UnknownPacketException(id);
        }
        return codec;
    }

    @Override
    public ByteBuf writeHeader(PacketCodec<?> codec, ByteBuf data) {
        ByteBuf buf = Unpooled.buffer(1);
        buf.writeByte(codec.getOpcode());
        return buf;
    }

    @Override
    public CodecLookupService getCodecLookupService() {
        return MinecraftCodecLookupService.INSTANCE;
    }

    @Override
    public Packet getHandshakePacket(String username) {
        return new PlayerHandshakePacket((byte)this.getVersion(), username, "localhost", 25565);
    }

    @Override
    public int getVersion() {
        return 49;
    }
}

