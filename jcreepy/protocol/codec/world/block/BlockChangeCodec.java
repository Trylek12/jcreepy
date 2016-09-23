
package jcreepy.protocol.codec.world.block;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.world.block.BlockChangePacket;

public final class BlockChangeCodec
extends PacketCodec<BlockChangePacket> {
    public BlockChangeCodec() {
        super(BlockChangePacket.class, 53);
    }

    @Override
    public BlockChangePacket decode(ByteBuf buffer) throws IOException {
        int x = buffer.readInt();
        short y = buffer.readUnsignedByte();
        int z = buffer.readInt();
        short type = buffer.readShort();
        short metadata = buffer.readUnsignedByte();
        return new BlockChangePacket(x, y, z, type, metadata);
    }

    @Override
    public ByteBuf encode(BlockChangePacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(12);
        buffer.writeInt(packet.getX());
        buffer.writeByte(packet.getY());
        buffer.writeInt(packet.getZ());
        buffer.writeShort(packet.getType());
        buffer.writeByte(packet.getMetadata());
        return buffer;
    }
}

