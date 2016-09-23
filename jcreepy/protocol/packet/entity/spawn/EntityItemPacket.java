
package jcreepy.protocol.packet.entity.spawn;

import jcreepy.math.Vector3;
import jcreepy.protocol.packet.entity.EntityPacket;

public final class EntityItemPacket
extends EntityPacket {
    private final int x;
    private final int y;
    private final int z;
    private final int rotation;
    private final int pitch;
    private final int roll;
    private final int itemId;
    private final int count;
    private final short damage;

    public EntityItemPacket(int id, int itemId, int count, short damage, Vector3 pos, int rotation, int pitch, int roll) {
        this(id, itemId, count, damage, (int)pos.getX(), (int)pos.getY(), (int)pos.getZ(), rotation, pitch, roll);
    }

    public EntityItemPacket(int id, int itemId, int count, short damage, int x, int y, int z, int rotation, int pitch, int roll) {
        super(id);
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotation = rotation;
        this.pitch = pitch;
        this.roll = roll;
        this.itemId = itemId;
        this.count = count;
        this.damage = damage;
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

    public int getRotation() {
        return this.rotation;
    }

    public int getPitch() {
        return this.pitch;
    }

    public int getRoll() {
        return this.roll;
    }

    public int getId() {
        return this.itemId;
    }

    public int getCount() {
        return this.count;
    }

    public short getDamage() {
        return this.damage;
    }
}

