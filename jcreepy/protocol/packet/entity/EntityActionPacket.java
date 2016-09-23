
package jcreepy.protocol.packet.entity;

import jcreepy.protocol.packet.entity.EntityPacket;

public final class EntityActionPacket
extends EntityPacket {
    public static final int ACTION_CROUCH = 1;
    public static final int ACTION_UNCROUCH = 2;
    public static final int ACTION_LEAVE_BED = 3;
    public static final int ACTION_START_SPRINTING = 4;
    public static final int ACTION_STOP_SPRINTING = 5;
    private final int action;

    public EntityActionPacket(int id, int action) {
        super(id);
        this.action = action;
    }

    public int getAction() {
        return this.action;
    }
}

