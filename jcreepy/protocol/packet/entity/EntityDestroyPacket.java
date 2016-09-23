
package jcreepy.protocol.packet.entity;

import jcreepy.network.Packet;

public final class EntityDestroyPacket
extends Packet {
    private final int[] id;

    public EntityDestroyPacket(int[] id) {
        this.id = id;
    }

    public int[] getId() {
        return this.id;
    }
}

