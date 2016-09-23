
package jcreepy.protocol.packet.player;

import jcreepy.protocol.packet.entity.EntityPacket;
import jcreepy.world.block.Block;
import jcreepy.world.entity.Entity;

public final class PlayerBedPacket
extends EntityPacket {
    private final int used;
    private final int x;
    private final int y;
    private final int z;

    public PlayerBedPacket(Entity entity, Block head) {
        this(entity.getId(), 0, head.getX(), head.getY(), head.getZ());
    }

    public PlayerBedPacket(int id, int used, int x, int y, int z) {
        super(id);
        this.used = used;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getUsed() {
        return this.used;
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
}

