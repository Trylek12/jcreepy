
package jcreepy.protocol.packet.player;

import jcreepy.network.Packet;

public final class PlayerStatisticPacket
extends Packet {
    private final int id;
    private final byte amount;

    public PlayerStatisticPacket(int id, byte amount) {
        this.id = id;
        this.amount = amount;
    }

    public int getId() {
        return this.id;
    }

    public byte getAmount() {
        return this.amount;
    }
}

