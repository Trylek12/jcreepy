
package jcreepy.protocol.packet.entity.spawn;

import jcreepy.protocol.packet.entity.EntityPacket;

public final class EntityObjectPacket
extends EntityPacket {
    private final byte type;
    private final int x;
    private final int y;
    private final int z;
    private final int throwerId;
    private final short speedX;
    private final short speedY;
    private final short speedZ;

    public EntityObjectPacket(int entityId, byte type, int x, int y, int z, int throwerId, short speedX, short speedY, short speedZ) {
        super(entityId);
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.throwerId = throwerId;
        this.speedX = speedX;
        this.speedY = speedY;
        this.speedZ = speedZ;
    }

    public EntityObjectPacket(int entityId, byte type, int x, int y, int z, int throwerId) {
        this(entityId, type, x, y, z, throwerId, 0, 0, 0);
    }

    public byte getType() {
        return this.type;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public int getThrowerId() {
        return this.throwerId;
    }

    public short getSpeedX() {
        return this.speedX;
    }

    public short getSpeedY() {
        return this.speedY;
    }

    public short getSpeedZ() {
        return this.speedZ;
    }
}

