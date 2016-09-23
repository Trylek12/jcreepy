
package jcreepy.protocol.packet.world;

import jcreepy.network.Packet;

public final class EffectPacket
extends Packet {
    private final int id;
    private final int x;
    private final int y;
    private final int z;
    private final int data;
    private final boolean volumeDecrease;

    public EffectPacket(int id, int x, int y, int z, int data, boolean volumeDecrease) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.data = data;
        this.volumeDecrease = volumeDecrease;
    }

    public int getId() {
        return this.id;
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

    public int getData() {
        return this.data;
    }

    public boolean hasVolumeDecrease() {
        return this.volumeDecrease;
    }
}

