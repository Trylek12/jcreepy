
package jcreepy.protocol.packet.player;

import jcreepy.protocol.packet.entity.EntityPacket;

public final class PlayerCollectItemPacket
extends EntityPacket {
    private final int collector;

    public PlayerCollectItemPacket(int id, int collector) {
        super(id);
        this.collector = collector;
    }

    public int getCollector() {
        return this.collector;
    }
}

