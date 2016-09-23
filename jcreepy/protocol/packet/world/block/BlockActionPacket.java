
package jcreepy.protocol.packet.world.block;

import jcreepy.network.Packet;

public final class BlockActionPacket
extends Packet {
    private final int x;
    private final int y;
    private final int z;
    private final byte firstByte;
    private final byte secondByte;
    private final short blockId;

    public BlockActionPacket(int x, int y, int z, short blockId, byte firstByte, byte secondByte) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.firstByte = firstByte;
        this.secondByte = secondByte;
        this.blockId = blockId;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public int getFirstByte() {
        return this.firstByte;
    }

    public int getSecondByte() {
        return this.secondByte;
    }

    public short getBlockId() {
        return this.blockId;
    }
}

