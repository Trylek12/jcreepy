
package jcreepy.protocol.packet.world.block;

import jcreepy.network.Packet;

public final class BlockBreakAnimationPacket
extends Packet {
    private final int entityId;
    private final int x;
    private final int y;
    private final int z;
    private final byte stage;

    public BlockBreakAnimationPacket(int entityId, int x, int y, int z, byte stage) {
        this.entityId = entityId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.stage = stage;
    }

    public int getEntityId() {
        return this.entityId;
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

    public byte getStage() {
        return this.stage;
    }
}

