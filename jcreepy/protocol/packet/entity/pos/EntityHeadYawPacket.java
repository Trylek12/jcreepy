
package jcreepy.protocol.packet.entity.pos;

import jcreepy.protocol.packet.entity.EntityPacket;

public final class EntityHeadYawPacket
extends EntityPacket {
    private final int headYaw;

    public EntityHeadYawPacket(int id, int headYaw) {
        super(id);
        this.headYaw = headYaw;
    }

    public int getHeadYaw() {
        return this.headYaw;
    }
}

