
package jcreepy.protocol.packet.player;

import jcreepy.network.Packet;

public final class PlayerGroundPacket
extends Packet {
    private final boolean onGround;

    public PlayerGroundPacket(boolean onGround) {
        this.onGround = onGround;
    }

    public boolean isOnGround() {
        return this.onGround;
    }
}

