
package jcreepy.protocol.packet.player;

import jcreepy.network.Packet;

public final class PlayerChatPacket
extends Packet {
    private final String message;

    public PlayerChatPacket(String message) {
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }

    @Override
    public boolean isAsync() {
        return false;
    }
}

