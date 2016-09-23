
package jcreepy.protocol.packet.world.block;

import jcreepy.network.Packet;

public final class BlockChangePacket
extends Packet {
    private final int x;
    private final int y;
    private final int z;
    private final int metadata;
    private final short type;

    public BlockChangePacket(int x, int y, int z, short type, int metadata) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
        this.metadata = metadata;
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

    public short getType() {
        return this.type;
    }

    public int getMetadata() {
        return this.metadata;
    }
}

