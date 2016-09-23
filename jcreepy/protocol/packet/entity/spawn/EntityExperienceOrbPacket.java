
package jcreepy.protocol.packet.entity.spawn;

import jcreepy.protocol.packet.entity.EntityPacket;

public final class EntityExperienceOrbPacket
extends EntityPacket {
    private final int x;
    private final int y;
    private final int z;
    private final short count;

    public EntityExperienceOrbPacket(int id, int x, int y, int z, short count) {
        super(id);
        this.x = x;
        this.y = y;
        this.z = z;
        this.count = count;
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

    public short getCount() {
        return this.count;
    }
}

