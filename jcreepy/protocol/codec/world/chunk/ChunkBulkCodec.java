
package jcreepy.protocol.codec.world.chunk;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.zip.Deflater;
import jcreepy.math.BitSize;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.world.chunk.ChunkBulkPacket;
import jcreepy.world.block.Chunk;

public final class ChunkBulkCodec
extends PacketCodec<ChunkBulkPacket> {
    private static final int COMPRESSION_LEVEL = 1;

    public ChunkBulkCodec() {
        super(ChunkBulkPacket.class, 56);
    }

    @Override
    public ChunkBulkPacket decode(ByteBuf buffer) throws IOException {
        int length = buffer.readShort();
        int compressed = buffer.readInt();
        byte[] compressedDataFlat = new byte[compressed];
        buffer.readBytes(compressedDataFlat);
        int[] x = new int[length];
        int[] z = new int[length];
        byte[][][] data = new byte[length][][];
        boolean[][] hasAdd = new boolean[length][];
        byte[][] biomeData = new byte[length][];
        for (int i = 0; i < length; ++i) {
            x[i] = buffer.readInt();
            z[i] = buffer.readInt();
            short primaryBitmap = buffer.readShort();
            hasAdd[i] = ChunkBulkCodec.shortToBooleanArray(buffer.readShort());
            data[i] = ChunkBulkCodec.shortToByteByteArray(primaryBitmap, hasAdd[i]);
            biomeData[i] = new byte[Chunk.BLOCKS.AREA];
        }
        return new ChunkBulkPacket();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public ByteBuf encode(ChunkBulkPacket message) throws IOException {
        int length;
        int compressed;
        ByteBuf buffer;
        byte[] compressedDataFlat;
        buffer = Unpooled.buffer();
        length = message.getX().length;
        buffer.writeShort(length);
        int dataLength = 0;
        byte[][][] uncompressedData = message.getData();
        byte[][] biomeData = message.getBiomeData();
        for (int i = 0; i < length; ++i) {
            byte[][] uncompressedColumnData = uncompressedData[i];
            for (int j = 0; j < uncompressedColumnData.length; ++j) {
                byte[] uncompressedChunkData = uncompressedColumnData[j];
                if (uncompressedChunkData == null) continue;
                dataLength += uncompressedChunkData.length;
            }
            dataLength += biomeData[i].length;
        }
        byte[] uncompressedDataFlat = new byte[dataLength];
        int pos = 0;
        for (int i2 = 0; i2 < length; ++i2) {
            byte[][] uncompressedColumnData = uncompressedData[i2];
            for (int j = 0; j < uncompressedColumnData.length; ++j) {
                byte[] uncompressedChunkData = uncompressedColumnData[j];
                if (uncompressedChunkData == null) continue;
                int copyLength = uncompressedChunkData.length;
                System.arraycopy(uncompressedChunkData, 0, uncompressedDataFlat, pos, copyLength);
                pos += copyLength;
            }
            System.arraycopy(biomeData[i2], 0, uncompressedDataFlat, pos, biomeData[i2].length);
            pos += biomeData[i2].length;
        }
        if (pos != dataLength) {
            throw new IllegalStateException("Flat data length miscalculated");
        }
        Deflater deflater = new Deflater(1);
        deflater.setInput(uncompressedDataFlat);
        deflater.finish();
        compressedDataFlat = new byte[uncompressedDataFlat.length + 1024];
        compressed = deflater.deflate(compressedDataFlat);
        try {
            if (compressed == 0) {
                throw new IOException("Not all bytes compressed.");
            }
        }
        finally {
            deflater.end();
        }
        buffer.writeInt(compressed);
        buffer.writeBytes(compressedDataFlat, 0, compressed);
        for (int i = 0; i < length; ++i) {
            buffer.writeInt(message.getX()[i]);
            buffer.writeInt(message.getZ()[i]);
            buffer.writeShort(ChunkBulkCodec.byteByteArrayToShort(message.getData()[i]));
            buffer.writeShort(ChunkBulkCodec.booleanArrayToShort(message.hasAdditionalData()[i]));
        }
        return buffer;
    }

    private static short booleanArrayToShort(boolean[] array) {
        short s = 0;
        for (int i = 0; i < array.length; ++i) {
            if (!array[i]) continue;
            s = (short)(s | 1 << i);
        }
        return s;
    }

    private static boolean[] shortToBooleanArray(short s) {
        boolean[] array = new boolean[16];
        for (int i = 0; i < 16; ++i) {
            array[i] = (s & 1 << i) != 0;
        }
        return array;
    }

    private static short byteByteArrayToShort(byte[][] array) {
        short s = 0;
        for (int i = 0; i < array.length; ++i) {
            if (array[i] == null) continue;
            s = (short)(s | 1 << i);
        }
        return s;
    }

    private static byte[][] shortToByteByteArray(short s, boolean[] add) {
        byte[][] array = new byte[16][];
        for (int i = 0; i < 16; ++i) {
            if ((s & 1 << i) != 0) {
                int length = 5 * Chunk.BLOCKS.HALF_VOLUME;
                if (add[i]) {
                    length += Chunk.BLOCKS.HALF_VOLUME;
                }
                array[i] = new byte[length];
                continue;
            }
            array[i] = null;
        }
        return array;
    }
}

