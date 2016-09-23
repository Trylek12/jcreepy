
package jcreepy.protocol.packet.entity.pos;

import jcreepy.math.Vector3;
import jcreepy.protocol.packet.entity.EntityPacket;

public final class EntityVelocityPacket
extends EntityPacket {
    private final int velocityX;
    private final int velocityY;
    private final int velocityZ;

    public EntityVelocityPacket(int id, Vector3 velocity) {
        this(id, (int)velocity.getX(), (int)velocity.getY(), (int)velocity.getZ());
    }

    public EntityVelocityPacket(int id, int velocityX, int velocityY, int velocityZ) {
        super(id);
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
    }

    public int getVelocityX() {
        return this.velocityX;
    }

    public int getVelocityY() {
        return this.velocityY;
    }

    public int getVelocityZ() {
        return this.velocityZ;
    }
}

