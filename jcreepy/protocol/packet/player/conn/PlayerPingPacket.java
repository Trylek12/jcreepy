
package jcreepy.protocol.packet.player.conn;

import jcreepy.network.Packet;

public final class PlayerPingPacket
extends Packet {
    private final int pingId;

    public PlayerPingPacket(int pingId) {
        this.pingId = pingId;
    }

    public int getPingId() {
        return this.pingId;
    }
}

