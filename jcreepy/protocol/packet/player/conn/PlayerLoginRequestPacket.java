
package jcreepy.protocol.packet.player.conn;

import jcreepy.network.Packet;

public final class PlayerLoginRequestPacket
extends Packet {
    private final int id;
    private final byte dimension;
    private final byte mode;
    private final byte difficulty;
    private final String worldType;
    private final short maxPlayers;

    public PlayerLoginRequestPacket(int id, String worldType, byte mode, byte dimension, byte difficulty, short maxPlayers) {
        this.id = id;
        this.worldType = worldType;
        this.mode = mode;
        this.dimension = dimension;
        this.difficulty = difficulty;
        this.maxPlayers = maxPlayers;
    }

    public int getId() {
        return this.id;
    }

    public String getWorldType() {
        return this.worldType;
    }

    public byte getGameMode() {
        return this.mode;
    }

    public byte getDimension() {
        return this.dimension;
    }

    public byte getDifficulty() {
        return this.difficulty;
    }

    public short getMaxPlayers() {
        return this.maxPlayers;
    }
}

