
package jcreepy.protocol.packet.player;

import jcreepy.network.Packet;

public final class PlayerAbilityPacket
extends Packet {
    private final boolean godMode;
    private final boolean isFlying;
    private final boolean canFly;
    private final boolean creativeMode;
    private final byte flyingSpeed;
    private final byte walkingSpeed;

    public PlayerAbilityPacket(boolean godMode, boolean isFlying, boolean canFly, boolean creativeMode, byte flyingSpeed, byte walkingSpeed) {
        this.godMode = godMode;
        this.isFlying = isFlying;
        this.canFly = canFly;
        this.creativeMode = creativeMode;
        this.flyingSpeed = flyingSpeed;
        this.walkingSpeed = walkingSpeed;
    }

    public boolean isGodMode() {
        return this.godMode;
    }

    public boolean isFlying() {
        return this.isFlying;
    }

    public boolean canFly() {
        return this.canFly;
    }

    public boolean isCreativeMode() {
        return this.creativeMode;
    }

    public byte getFlyingSpeed() {
        return this.flyingSpeed;
    }

    public byte getWalkingSpeed() {
        return this.walkingSpeed;
    }
}

