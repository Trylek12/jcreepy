
package jcreepy.protocol.packet.world.chunk;

import jcreepy.network.Packet;

public final class ChunkDataPacket
extends Packet {
    private final int x;
    private final int z;
    private final boolean contiguous;
    private final boolean[] hasAdditionalData;
    private final byte[][] data;
    private final byte[] biomeData;
    private final boolean unload;

    public ChunkDataPacket(int x, int z, boolean contiguous, boolean[] hasAdditionalData, byte[][] data, byte[] biomeData) {
        this(x, z, contiguous, hasAdditionalData, data, biomeData, false);
    }

    public ChunkDataPacket(int x, int z, boolean contiguous, boolean[] hasAdditionalData, byte[][] data, byte[] biomeData, boolean unload) {
        if (!(unload || hasAdditionalData.length == data.length && data.length == 16)) {
            throw new IllegalArgumentException("Data and hasAdditionalData must have a length of 16");
        }
        this.x = x;
        this.z = z;
        this.contiguous = contiguous;
        this.hasAdditionalData = hasAdditionalData;
        this.data = data;
        this.biomeData = biomeData;
        this.unload = unload;
    }

    public int getX() {
        return this.x;
    }

    public int getZ() {
        return this.z;
    }

    public boolean[] hasAdditionalData() {
        return this.hasAdditionalData;
    }

    public boolean isContiguous() {
        return this.contiguous;
    }

    public byte[][] getData() {
        return this.data;
    }

    public byte[] getBiomeData() {
        return this.biomeData;
    }

    public boolean shouldUnload() {
        return this.unload;
    }
}

