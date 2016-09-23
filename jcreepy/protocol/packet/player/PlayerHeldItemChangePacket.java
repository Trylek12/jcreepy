
package jcreepy.protocol.packet.player;

import jcreepy.network.Packet;

public final class PlayerHeldItemChangePacket
extends Packet {
    private final int slot;

    public PlayerHeldItemChangePacket(int slot) {
        this.slot = slot;
    }

    public int getSlot() {
        return this.slot;
    }
}

