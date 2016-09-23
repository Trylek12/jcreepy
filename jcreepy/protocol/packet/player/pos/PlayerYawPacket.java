
package jcreepy.protocol.packet.player.pos;

import jcreepy.network.Packet;

public final class PlayerYawPacket
extends Packet {
    private final float yaw;
    private final float pitch;
    private final float roll = 0.0f;
    private final boolean onGround;

    public PlayerYawPacket(float yaw, float pitch, boolean onGround) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public float getRoll() {
        return this.roll;
    }

    public boolean isOnGround() {
        return this.onGround;
    }
}

