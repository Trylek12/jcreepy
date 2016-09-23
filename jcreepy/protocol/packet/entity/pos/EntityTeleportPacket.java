
package jcreepy.protocol.packet.entity.pos;

import jcreepy.math.Vector3;
import jcreepy.protocol.packet.entity.EntityPacket;

public final class EntityTeleportPacket
extends EntityPacket {
    private final int x;
    private final int y;
    private final int z;
    private final int rotation;
    private final int pitch;

    public EntityTeleportPacket(int id, Vector3 position, int rotation, int pitch) {
        this(id, (int)position.getX(), (int)position.getY(), (int)position.getZ(), rotation, pitch);
    }

    public EntityTeleportPacket(int id, int x, int y, int z, int rotation, int pitch) {
        super(id);
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotation = rotation;
        this.pitch = pitch;
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
}

