
package jcreepy.protocol.codec.world.block;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.world.block.BlockBulkPacket;

public final class BlockBulkCodec
extends PacketCodec<BlockBulkPacket> {
    public BlockBulkCodec() {
        super(BlockBulkPacket.class, 52);
    }

    @Override
    public BlockBulkPacket decode(ByteBuf buffer) throws IOException {
        int chunkX = buffer.readInt();
        int chunkZ = buffer.readInt();
        int changes = buffer.readUnsignedShort();
        int dataLength = buffer.readInt();
        if (dataLength != changes << 2) {
            throw new IllegalStateException("data length and record count mismatch");
        }
        short[] coordinates = new short[changes * 3];
        short[] types = new short[changes];
        byte[] metadata = new byte[changes];
        int coordinateIndex = 0;
        for (int i = 0; i < changes; ++i) {
            int record = buffer.readInt();
            coordinates[coordinateIndex++] = (short)(record >> 28 & 15);
            coordinates[coordinateIndex++] = (short)(record >> 16 & 255);
            coordinates[coordinateIndex++] = (short)(record >> 24 & 15);
            types[i] = (short)(record >> 4 & 4095);
            metadata[i] = (byte)(record & 15);
        }
        return new BlockBulkPacket(chunkX, chunkZ, coordinates, types, metadata);
    }

    @Override
    public ByteBuf encode(BlockBulkPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(10 + packet.getChanges() * 4);
        buffer.writeInt(packet.getChunkX());
        buffer.writeInt(packet.getChunkZ());
        buffer.writeShort(packet.getChanges());
        buffer.writeInt(packet.getChanges() << 2);
        int changes = packet.getChanges();
        byte[] metadata = packet.getMetadata();
        short[] types = packet.getTypes();
        short[] coordinates = packet.getCoordinates();
        int coordinateIndex = 0;
        for (int i = 0; i < changes; ++i) {
            int record = metadata[i] & 15;
            record |= (types[i] & 4095) << 4;
            record |= (coordinates[coordinateIndex++] & 15) << 28;
            record |= (coordinates[coordinateIndex++] & 255) << 16;
            buffer.writeInt(record |= (coordinates[coordinateIndex++] & 15) << 24);
        }
        return buffer;
    }
}

