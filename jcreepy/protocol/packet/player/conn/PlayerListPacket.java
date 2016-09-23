
package jcreepy.protocol.packet.player.conn;

import jcreepy.network.Packet;

public final class PlayerListPacket
extends Packet {
    private final String playerName;
    private final boolean playerOnline;
    private final short ping;

    public PlayerListPacket(String playerName, boolean playerOnline, short ping) {
        this.playerName = playerName;
        this.playerOnline = playerOnline;
        this.ping = ping;
    }

    public String getPlayerName() {
        return this.playerName;
    }

    public boolean playerIsOnline() {
        return this.playerOnline;
    }

    public short getPing() {
        return this.ping;
    }
}

