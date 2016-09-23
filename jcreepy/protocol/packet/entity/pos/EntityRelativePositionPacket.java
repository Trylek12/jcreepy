
package jcreepy.protocol.packet.entity.pos;

import jcreepy.math.Vector3;
import jcreepy.protocol.packet.entity.EntityPacket;

public final class EntityRelativePositionPacket
extends EntityPacket {
    private final int deltaX;
    private final int deltaY;
    private final int deltaZ;

    public EntityRelativePositionPacket(int id, Vector3 deltaPosition) {
        this(id, (int)deltaPosition.getX(), (int)deltaPosition.getY(), (int)deltaPosition.getZ());
    }

    public EntityRelativePositionPacket(int id, int deltaX, int deltaY, int deltaZ) {
        super(id);
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.deltaZ = deltaZ;
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
}

