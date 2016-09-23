
package jcreepy.protocol.packet.entity.pos;

import jcreepy.math.Vector3;
import jcreepy.protocol.packet.entity.EntityPacket;

public final class EntityRelativePositionYawPacket
extends EntityPacket {
    private final int deltaX;
    private final int deltaY;
    private final int deltaZ;
    private final int rotation;
    private final int pitch;

    public EntityRelativePositionYawPacket(int id, Vector3 deltaPosition, int rotation, int pitch) {
        this(id, (int)deltaPosition.getX(), (int)deltaPosition.getY(), (int)deltaPosition.getZ(), rotation, pitch);
    }

    public EntityRelativePositionYawPacket(int id, int deltaX, int deltaY, int deltaZ, int rotation, int pitch) {
        super(id);
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.deltaZ = deltaZ;
        this.rotation = rotation;
        this.pitch = pitch;
    }

    public int getDeltaX() {
        return this.deltaX;
    }

    public int getDeltaY() {
        return this.deltaY;
    }

    public int getDeltaZ() {
        return this.deltaZ;
    }

    public int getRotation() {
        return this.rotation;
    }

    public int getPitch() {
        return this.pitch;
    }
}

