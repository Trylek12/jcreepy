
package jcreepy.protocol.packet.entity.spawn;

import jcreepy.protocol.packet.entity.EntityPacket;

public final class EntityThunderboltPacket
extends EntityPacket {
    private final int mode;
    private final int x;
    private final int y;
    private final int z;

    public EntityThunderboltPacket(int id, int mode, int x, int y, int z) {
        super(id);
        this.mode = mode;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getMode() {
        return this.mode;
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

