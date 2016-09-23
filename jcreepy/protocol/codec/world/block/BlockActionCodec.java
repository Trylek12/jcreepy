
package jcreepy.protocol.codec.world.block;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.world.block.BlockActionPacket;

public final class BlockActionCodec
extends PacketCodec<BlockActionPacket> {
    public BlockActionCodec() {
        super(BlockActionPacket.class, 54);
    }

    @Override
    public BlockActionPacket decode(ByteBuf buffer) throws IOException {
        int x = buffer.readInt();
        int y = buffer.readUnsignedShort();
        int z = buffer.readInt();
        byte firstByte = buffer.readByte();
        byte secondByte = buffer.readByte();
        short blockId = buffer.readShort();
        return new BlockActionPacket(x, y, z, blockId, firstByte, secondByte);
    }

    @Override
    public ByteBuf encode(BlockActionPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(14);
        buffer.writeInt(packet.getX());
        buffer.writeShort(packet.getY());
        buffer.writeInt(packet.getZ());
        buffer.writeByte(packet.getFirstByte());
        buffer.writeByte(packet.getSecondByte());
        buffer.writeShort(packet.getBlockId());
        return buffer;
    }
}

