
package jcreepy.protocol.packet.player.pos;

import jcreepy.network.Packet;
import jcreepy.protocol.packet.player.pos.PlayerPositionPacket;
import jcreepy.protocol.packet.player.pos.PlayerYawPacket;

public final class PlayerPositionYawPacket
extends Packet {
    private final PlayerPositionPacket position;
    private final PlayerYawPacket rotation;

    public PlayerPositionYawPacket(double x, double y, double z, double stance, float yaw, float pitch, boolean onGround) {
        this.position = new PlayerPositionPacket(x, y, z, stance, onGround);
        this.rotation = new PlayerYawPacket(yaw, pitch, onGround);
    }

    public PlayerPositionPacket getPlayerPositionMessage() {
        return this.position;
    }

    public PlayerYawPacket getPlayerLookMessage() {
        return this.rotation;
    }

    public double getX() {
        return this.position.getX();
    }

    public double getY() {
        return this.position.getY();
    }

    public double getStance() {
        return this.position.getStance();
    }

    public double getZ() {
        return this.position.getZ();
    }

    public float getYaw() {
        return this.rotation.getYaw();
    }

    public float getPitch() {
        return this.rotation.getPitch();
    }

    public boolean isOnGround() {
        return this.position.isOnGround();
    }
}

