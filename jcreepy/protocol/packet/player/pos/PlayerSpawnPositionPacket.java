
package jcreepy.protocol.packet.player.pos;

import jcreepy.network.Packet;

public final class PlayerSpawnPositionPacket
extends Packet {
    private final int x;
    private final int y;
    private final int z;

    public PlayerSpawnPositionPacket(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }
}

