
package jcreepy.protocol.packet.player;

import jcreepy.network.Packet;

public final class PlayerDiggingPacket
extends Packet {
    public static final int STATE_START_DIGGING = 0;
    public static final int STATE_DONE_DIGGING = 2;
    public static final int STATE_UPDATE_BLOCK = 3;
    public static final int STATE_DROP_ITEM = 4;
    public static final int STATE_SHOOT_ARROW_EAT_FOOD = 5;
    private final int state;
    private final int x;
    private final int y;
    private final int z;
    private final int face;

    public PlayerDiggingPacket(int state, int x, int y, int z, int face) {
        this.state = state;
        this.x = x;
        this.y = y;
        this.z = z;
        this.face = face;
    }

    public int getState() {
        return this.state;
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

    public int getFace() {
        return this.face;
    }
}

