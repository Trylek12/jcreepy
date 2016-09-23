
package jcreepy.protocol.packet.world.chunk;

import jcreepy.network.Packet;

public final class ChunkBulkPacket
extends Packet {
    private final int[] x;
    private final int[] z;
    private final boolean[][] addData;
    private final byte[][][] data;
    private final byte[][] biomeData;

    public ChunkBulkPacket() {
        this.x = new int[0];
        this.z = new int[0];
        this.addData = new boolean[0][0];
        this.data = new byte[0][0][0];
        this.biomeData = new byte[0][0];
    }

    public ChunkBulkPacket(int[] x, int[] z, boolean[][] hasAdditionalData, byte[][][] data, byte[][] biomeData) {
        int l = x.length;
        if (l != z.length || l != hasAdditionalData.length || l != data.length || l != biomeData.length) {
            throw new IllegalArgumentException("The lengths of all bulk data arrays must be equal");
        }
        this.x = x;
        this.z = z;
        this.addData = hasAdditionalData;
        this.data = data;
        this.biomeData = biomeData;
    }

    public int[] getX() {
        return this.x;
    }

    public int[] getZ() {
        return this.z;
    }

    public boolean[][] hasAdditionalData() {
        return this.addData;
    }

    public byte[][][] getData() {
        return this.data;
    }

    public byte[][] getBiomeData() {
        return this.biomeData;
    }
}

