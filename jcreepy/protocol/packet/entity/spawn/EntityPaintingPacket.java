
package jcreepy.protocol.packet.entity.spawn;

import jcreepy.protocol.packet.entity.EntityPacket;

public final class EntityPaintingPacket
extends EntityPacket {
    private final int x;
    private final int y;
    private final int z;
    private final int direction;
    private final String title;

    public EntityPaintingPacket(int entityId, String title, int x, int y, int z, int direction) {
        super(entityId);
        this.x = x;
        this.y = y;
        this.z = z;
        this.direction = direction;
        this.title = title;
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

    public int getDirection() {
        return this.direction;
    }

    public String getTitle() {
        return this.title;
    }
}

