
package jcreepy.protocol.packet.entity;

import jcreepy.protocol.packet.entity.EntityPacket;

public final class EntityAttachEntityPacket
extends EntityPacket {
    private final int vehicle;

    public EntityAttachEntityPacket(int id, int vehicle) {
        super(id);
        this.vehicle = vehicle;
    }

    public int getVehicle() {
        return this.vehicle;
    }
}

