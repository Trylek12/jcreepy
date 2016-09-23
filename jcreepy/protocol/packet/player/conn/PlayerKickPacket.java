
package jcreepy.protocol.packet.player.conn;

import jcreepy.network.Packet;

public final class PlayerKickPacket
extends Packet {
    private final String reason;

    public PlayerKickPacket(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return this.reason;
    }
}

