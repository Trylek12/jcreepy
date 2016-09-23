
package jcreepy.protocol.packet.entity;

import jcreepy.protocol.packet.entity.EntityPacket;

public final class EntityStatusPacket
extends EntityPacket {
    public static final byte ENTITY_HURT = 2;
    public static final byte ENTITY_DEAD = 3;
    public static final byte WOLF_TAMING = 6;
    public static final byte WOLF_TAMED = 7;
    public static final byte WOLF_SHAKING = 8;
    public static final byte EATING_ACCEPTED = 9;
    public static final byte SHEEP_EAT_GRASS = 10;
    private final byte status;

    public EntityStatusPacket(int id, byte status) {
        super(id);
        this.status = status;
    }

    public byte getStatus() {
        return this.status;
    }
}

