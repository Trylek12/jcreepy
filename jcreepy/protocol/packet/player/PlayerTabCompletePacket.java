
package jcreepy.protocol.packet.player;

import jcreepy.network.Packet;

public final class PlayerTabCompletePacket
extends Packet {
    private final String text;

    public PlayerTabCompletePacket(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }
}

