
package jcreepy.protocol.packet.entity;

import jcreepy.nbt.CompoundMap;
import jcreepy.network.Packet;

public final class EntityTileDataPacket
extends Packet {
    private final int x;
    private final int y;
    private final int z;
    private final int action;
    private int custom1;
    private int custom2;
    private int custom3;
    private CompoundMap data;

    public EntityTileDataPacket(int x, int y, int z, int action, int[] data) {
        this(x, y, z, action, data.length >= 1 ? data[0] : -1, data.length >= 2 ? data[1] : -1, data.length >= 3 ? data[2] : -1);
    }

    public EntityTileDataPacket(int x, int y, int z, int action, int custom1, int custom2, int custom3) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.action = action;
        this.custom1 = custom1;
        this.custom2 = custom2;
        this.custom3 = custom3;
    }

    public EntityTileDataPacket(int x, int y, int z, int action, CompoundMap data) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.action = action;
        this.data = data;
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

    public int getAction() {
        return this.action;
    }

    public int getCustom1() {
        return this.custom1;
    }

    public int getCustom2() {
        return this.custom2;
    }

    public int getCustom3() {
        return this.custom3;
    }

    public CompoundMap getData() {
        return this.data;
    }
}

