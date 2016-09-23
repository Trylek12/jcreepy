
package jcreepy.protocol.packet.entity.pos;

import jcreepy.protocol.packet.entity.EntityPacket;

public final class EntityYawPacket
extends EntityPacket {
    private final int rotation;
    private final int pitch;

    public EntityYawPacket(int id, int rotation, int pitch) {
        super(id);
        this.rotation = rotation;
        this.pitch = pitch;
    }

    public int getRotation() {
        return this.rotation;
    }

    public int getPitch() {
        return this.pitch;
    }
}

