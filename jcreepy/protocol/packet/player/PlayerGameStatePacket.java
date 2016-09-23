
package jcreepy.protocol.packet.player;

import jcreepy.data.GameMode;
import jcreepy.network.Packet;

public final class PlayerGameStatePacket
extends Packet {
    public static final byte INVALID_BED = 0;
    public static final byte BEGIN_RAINING = 1;
    public static final byte END_RAINING = 2;
    public static final byte CHANGE_GAME_MODE = 3;
    public static final byte ENTER_CREDITS = 4;
    private final byte reason;
    private final GameMode gameMode;

    public PlayerGameStatePacket(byte reason, GameMode gameMode) {
        this.reason = reason;
        this.gameMode = gameMode;
    }

    public PlayerGameStatePacket(byte reason) {
        this(reason, GameMode.CREATIVE);
    }

    public byte getReason() {
        return this.reason;
    }

    public GameMode getGameMode() {
        return this.gameMode;
    }
}

