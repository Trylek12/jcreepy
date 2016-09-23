
package jcreepy.protocol.packet.entity.effect;

import jcreepy.protocol.packet.entity.EntityPacket;

public final class EntityEffectPacket
extends EntityPacket {
    private final byte effect;
    private final byte amplifier;
    private final short duration;

    public EntityEffectPacket(int id, byte effect, byte amplifier, short duration) {
        super(id);
        this.effect = effect;
        this.amplifier = amplifier;
        this.duration = duration;
    }

    public byte getEffect() {
        return this.effect;
    }

    public byte getAmplifier() {
        return this.amplifier;
    }

    public short getDuration() {
        return this.duration;
    }
}

