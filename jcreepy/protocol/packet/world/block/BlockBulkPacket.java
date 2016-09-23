
package jcreepy.protocol.packet.world.block;

import jcreepy.network.Packet;

public final class BlockBulkPacket
extends Packet {
    private final int chunkX;
    private final int chunkZ;
    private final short[] coordinates;
    private final short[] types;
    private final byte[] metadata;

    public BlockBulkPacket(int chunkX, int chunkZ, short[] coordinates, short[] types, byte[] metadata) {
        if (coordinates.length != types.length * 3 || types.length != metadata.length) {
            throw new IllegalArgumentException();
        }
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.coordinates = coordinates;
        this.types = types;
        this.metadata = metadata;
    }

    public int getChunkX() {
        return this.chunkX;
    }

    public int getChunkZ() {
        return this.chunkZ;
    }

    public int getChanges() {
        return this.types.length;
    }

    public short[] getCoordinates() {
        return this.coordinates;
    }

    public short[] getTypes() {
        return this.types;
    }

    public byte[] getMetadata() {
        return this.metadata;
    }
}

