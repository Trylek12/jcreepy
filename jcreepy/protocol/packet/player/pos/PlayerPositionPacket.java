
package jcreepy.protocol.packet.player.pos;

import jcreepy.math.Vector3;
import jcreepy.network.Packet;

public final class PlayerPositionPacket
extends Packet {
    private final double x;
    private final double y;
    private final double z;
    private final double stance;
    private final boolean onGround;

    public PlayerPositionPacket(double x, double y, double z, double stance, boolean onGround) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.stance = stance;
        this.onGround = onGround;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public Vector3 getPosition() {
        return new Vector3(this.x, this.y, this.z);
    }

    public double getStance() {
        return this.stance;
    }

    public double getZ() {
        return this.z;
    }

    public boolean isOnGround() {
        return this.onGround;
    }
}

