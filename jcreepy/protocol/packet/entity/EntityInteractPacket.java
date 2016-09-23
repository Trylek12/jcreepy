
package jcreepy.protocol.packet.entity;

import jcreepy.protocol.packet.entity.EntityPacket;

public final class EntityInteractPacket
extends EntityPacket {
    private final int target;
    private final boolean punching;

    public EntityInteractPacket(int id, int target, boolean punching) {
        super(id);
        this.target = target;
        this.punching = punching;
    }

    public int getTarget() {
        return this.target;
    }

    public boolean isPunching() {
        return this.punching;
    }
}

