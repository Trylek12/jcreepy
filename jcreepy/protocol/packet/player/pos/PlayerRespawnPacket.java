
package jcreepy.protocol.packet.player.pos;

import jcreepy.network.Packet;

public final class PlayerRespawnPacket
extends Packet {
    private final byte difficulty;
    private final byte mode;
    private final int worldHeight;
    private final int dimension;
    private final String worldType;

    public PlayerRespawnPacket(int dimension, byte difficulty, byte mode, int worldHeight, String worldType) {
        this.dimension = dimension;
        this.difficulty = difficulty;
        this.mode = mode;
        this.worldHeight = worldHeight;
        this.worldType = worldType;
    }

    public PlayerRespawnPacket() {
        this(0, 0, 0, 0, "");
    }

    public int getDimension() {
        return this.dimension;
    }

    public byte getDifficulty() {
        return this.difficulty;
    }

    public byte getGameMode() {
        return this.mode;
    }

    public int getWorldHeight() {
        return this.worldHeight;
    }

    public String getWorldType() {
        return this.worldType;
    }
}

