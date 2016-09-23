
package jcreepy.protocol.packet.player;

import jcreepy.inventory.ItemStack;
import jcreepy.math.Vector3;
import jcreepy.network.Packet;

public final class PlayerBlockPlacementPacket
extends Packet {
    private final int x;
    private final int y;
    private final int z;
    private final Vector3 face;
    private final int direction;
    private final ItemStack heldItem;

    public PlayerBlockPlacementPacket(int x, int y, int z, int direction, Vector3 face) {
        this(x, y, z, direction, face, null);
    }

    public PlayerBlockPlacementPacket(int x, int y, int z, int direction, Vector3 face, ItemStack heldItem) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.face = face;
        this.direction = direction;
        this.heldItem = heldItem;
    }

    public ItemStack getHeldItem() {
        return this.heldItem;
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

    public int getDirection() {
        return this.direction;
    }

    public Vector3 getFace() {
        return this.face;
    }
}

