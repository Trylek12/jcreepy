
package jcreepy.protocol.packet.player;

import jcreepy.network.Packet;

public final class PlayerStatusPacket
extends Packet {
    public static final byte INITIAL_SPAWN = 0;
    public static final byte RESPAWN = 1;
    private final byte status;

    public PlayerStatusPacket(byte status) {
        this.status = status;
    }

    public byte getStatus() {
        return this.status;
    }
}

