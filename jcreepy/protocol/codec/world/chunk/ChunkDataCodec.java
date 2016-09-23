
package jcreepy.protocol.codec.world.chunk;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import jcreepy.math.BitSize;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.world.chunk.ChunkDataPacket;
import jcreepy.world.block.Chunk;

public final class ChunkDataCodec
extends PacketCodec<ChunkDataPacket> {
    private static final int COMPRESSION_LEVEL = 1;
    private static final int MAX_SECTIONS = 16;
    private final byte[] UNLOAD_COMPRESSED = new byte[]{120, -100, 99, 100, 28, -39, 0, 0, -127, -128, 1, 1};

    public ChunkDataCodec() {
        super(ChunkDataPacket.class, 51);
    }

    @Override
    public ChunkDataPacket decode(ByteBuf buffer) throws IOException {
        int x;
        boolean contiguous;
        byte[] uncompressedData;
        int z;
        int size;
        byte[][] data;
        boolean[] hasAdditionalData;
        x = buffer.readInt();
        z = buffer.readInt();
        contiguous = buffer.readByte() == 1;
        short primaryBitMap = buffer.readShort();
        short addBitMap = buffer.readShort();
        int compressedSize = buffer.readInt();
        byte[] compressedData = new byte[compressedSize];
        buffer.readBytes(compressedData);
        hasAdditionalData = new boolean[16];
        data = new byte[16][];
        size = 0;
        for (int i = 0; i < 16; ++i) {
            if ((primaryBitMap & 1 << i) == 0) continue;
            int sectionSize = Chunk.BLOCKS.HALF_VOLUME * 5;
            if ((addBitMap & 1 << i) != 0) {
                hasAdditionalData[i] = true;
                sectionSize += Chunk.BLOCKS.HALF_VOLUME;
            }
            data[i] = new byte[sectionSize];
            size += sectionSize;
        }
        if (contiguous) {
            size += Chunk.BLOCKS.AREA;
        }
        uncompressedData = new byte[size];
        Inflater inflater = new Inflater();
        inflater.setInput(compressedData);
        inflater.getRemaining();
        try {
            int uncompressed = inflater.inflate(uncompressedData);
            if (uncompressed == 0) {
                throw new IOException("Not all bytes uncompressed.");
            }
        }
        catch (DataFormatException e) {
            e.printStackTrace();
            throw new IOException("Bad compressed data.");
        }
        finally {
            inflater.end();
        }
        size = 0;
        size = this.readSectionData(uncompressedData, size, data, 0, 4096);
        size = this.readSectionData(uncompressedData, size, data, 4096, 2048);
        size = this.readSectionData(uncompressedData, size, data, 6144, 2048);
        size = this.readSectionData(uncompressedData, size, data, 8192, 2048);
        byte[] biomeData = new byte[Chunk.BLOCKS.AREA];
        if (contiguous) {
            System.arraycopy(uncompressedData, size, biomeData, 0, biomeData.length);
            size += biomeData.length;
        }
        return new ChunkDataPacket(x, z, contiguous, hasAdditionalData, data, biomeData);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public ByteBuf encode(ChunkDataPacket packet) throws IOException {
        ByteBuf buffer;
        byte[] compressedData;
        int compressed;
        buffer = Unpooled.buffer();
        buffer.writeInt(packet.getX());
        buffer.writeInt(packet.getZ());
        buffer.writeByte(packet.isContiguous() ? 1 : 0);
        if (packet.shouldUnload()) {
            buffer.writeShort(0);
            buffer.writeShort(0);
            buffer.writeInt(this.UNLOAD_COMPRESSED.length);
            buffer.writeBytes(this.UNLOAD_COMPRESSED);
            return buffer;
        }
        int sectionsSentBitmap = 0;
        int additionalDataBitMap = 0;
        byte[][] data = packet.getData();
        int uncompressedSize = 0;
        for (int i = 0; i < 16; ++i) {
            if (data[i] == null) continue;
            sectionsSentBitmap = (short)(sectionsSentBitmap | 1 << i);
            if (packet.hasAdditionalData()[i]) {
                additionalDataBitMap = (short)(additionalDataBitMap | 1 << i);
            }
            uncompressedSize += data[i].length;
        }
        if (packet.isContiguous()) {
            uncompressedSize += packet.getBiomeData().length;
        }
        buffer.writeShort(sectionsSentBitmap);
        buffer.writeShort(additionalDataBitMap);
        byte[] uncompressedData = new byte[uncompressedSize];
        int index = 0;
        index = this.writeSectionData(data, 0, uncompressedData, index, 4096);
        index = this.writeSectionData(data, 4096, uncompressedData, index, 2048);
        index = this.writeSectionData(data, 6144, uncompressedData, index, 2048);
        index = this.writeSectionData(data, 8192, uncompressedData, index, 2048);
        if (packet.isContiguous()) {
            System.arraycopy(packet.getBiomeData(), 0, uncompressedData, index, packet.getBiomeData().length);
            index += packet.getBiomeData().length;
        }
        compressedData = new byte[uncompressedSize];
        Deflater deflater = new Deflater(1);
        deflater.setInput(uncompressedData);
        deflater.finish();
        compressed = deflater.deflate(compressedData);
        try {
            if (compressed == 0) {
                throw new IOException("Not all bytes compressed.");
            }
        }
        finally {
            deflater.end();
        }
        buffer.writeInt(compressed);
        buffer.writeBytes(compressedData, 0, compressed);
        return buffer;
    }

    private int readSectionData(byte[] data, int off, byte[][] target, int targetOff, int len) {
        for (byte[] sectionTarget : target) {
            if (sectionTarget == null) continue;
            for (int i = targetOff; i < targetOff + len && i < sectionTarget.length; ++i) {
                sectionTarget[i] = data[off++];
            }
        }
        return off;
    }

    private int writeSectionData(byte[][] data, int off, byte[] target, int targetOff, int len) {
        for (byte[] sectionData : data) {
            if (sectionData == null) continue;
            int j = off;
            for (int i = 0; i < len; ++i) {
                target[targetOff++] = sectionData[j++];
            }
        }
        return targetOff;
    }
}

