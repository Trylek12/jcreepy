
package jcreepy.protocol.codec.world.block;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.world.block.BlockBreakAnimationPacket;

public class BlockBreakAnimationCodec
extends PacketCodec<BlockBreakAnimationPacket> {
    public BlockBreakAnimationCodec() {
        super(BlockBreakAnimationPacket.class, 55);
    }

    @Override
    public BlockBreakAnimationPacket decode(ByteBuf buffer) throws IOException {
        int entityId = buffer.readInt();
        int x = buffer.readInt();
        int y = buffer.readInt();
        int z = buffer.readInt();
        byte stage = buffer.readByte();
        return new BlockBreakAnimationPacket(entityId, x, y, z, stage);
    }

    @Override
    public ByteBuf encode(BlockBreakAnimationPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(17);
        buffer.writeInt(packet.getEntityId());
        buffer.writeInt(packet.getX());
        buffer.writeInt(packet.getY());
        buffer.writeInt(packet.getZ());
        buffer.writeByte(packet.getStage());
        return buffer;
    }
}

