
package jcreepy.protocol.packet.entity;

import jcreepy.network.Packet;
import jcreepy.world.entity.Entity;

public abstract class EntityPacket
extends Packet {
    protected int id;

    public EntityPacket() {
    }

    public EntityPacket(Entity entity) {
        this(entity.getId());
    }

    public EntityPacket(int id) {
        this.id = id;
    }

    public int getEntityId() {
        return this.id;
    }
}

