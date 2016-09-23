
package jcreepy.protocol.packet.entity.effect;

import jcreepy.protocol.packet.entity.EntityPacket;

public final class EntityRemoveEffectPacket
extends EntityPacket {
    private final byte effect;

    public EntityRemoveEffectPacket(int id, byte effect) {
        super(id);
        this.effect = effect;
    }

    public byte getEffect() {
        return this.effect;
    }
}

