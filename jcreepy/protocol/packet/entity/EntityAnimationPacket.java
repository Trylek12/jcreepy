
package jcreepy.protocol.packet.entity;

import jcreepy.data.Animation;
import jcreepy.protocol.packet.entity.EntityPacket;

public final class EntityAnimationPacket
extends EntityPacket {
    private final byte animation;

    public EntityAnimationPacket(int id, byte animation) {
        super(id);
        this.animation = animation;
    }

    public byte getAnimationId() {
        return this.animation;
    }

    public Animation getAnimation() {
        return Animation.get(this.animation);
    }
}

