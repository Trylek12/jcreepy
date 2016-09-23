
package jcreepy.protocol.packet.player;

import jcreepy.network.Packet;

public final class PlayerExperiencePacket
extends Packet {
    private final float barValue;
    private final short level;
    private final short totalExp;

    public PlayerExperiencePacket(float barValue, short level, short totalExp) {
        this.barValue = barValue;
        this.level = level;
        this.totalExp = totalExp;
    }

    public float getBarValue() {
        return this.barValue;
    }

    public short getLevel() {
        return this.level;
    }

    public short getTotalExp() {
        return this.totalExp;
    }
}

