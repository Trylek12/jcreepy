
package jcreepy.protocol.packet.entity.spawn;

import java.util.List;
import jcreepy.math.Vector3;
import jcreepy.protocol.packet.entity.EntityPacket;
import jcreepy.protocol.util.Parameter;

public final class EntityMobPacket
extends EntityPacket {
    private final int type;
    private final int x;
    private final int y;
    private final int z;
    private final int yaw;
    private final int pitch;
    private final int headYaw;
    private final short velocityZ;
    private final short velocityX;
    private final short velocityY;
    private final List<Parameter<?>> parameters;

    public EntityMobPacket(int id, int type, Vector3 pos, int yaw, int pitch, int headYaw, short velocityZ, short velocityX, short velocityY, List<Parameter<?>> parameters) {
        this(id, type, (int)pos.getX(), (int)pos.getY(), (int)pos.getZ(), yaw, pitch, headYaw, velocityZ, velocityX, velocityY, parameters);
    }

    public EntityMobPacket(int id, int type, int x, int y, int z, int yaw, int pitch, int headYaw, short velocityZ, short velocityX, short velocityY, List<Parameter<?>> parameters) {
        super(id);
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.headYaw = headYaw;
        this.velocityZ = velocityZ;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.parameters = parameters;
    }

    public int getType() {
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

    public int getYaw() {
        return this.yaw;
    }

    public int getPitch() {
        return this.pitch;
    }

    public int getHeadYaw() {
        return this.headYaw;
    }

    public short getVelocityZ() {
        return this.velocityZ;
    }

    public short getVelocityX() {
        return this.velocityX;
    }

    public short getVelocityY() {
        return this.velocityY;
    }

    public List<Parameter<?>> getParameters() {
        return this.parameters;
    }
}

