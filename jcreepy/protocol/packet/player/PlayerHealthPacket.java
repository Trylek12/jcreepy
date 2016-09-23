
package jcreepy.protocol.packet.player;

import jcreepy.network.Packet;

public final class PlayerHealthPacket
extends Packet {
    private final short health;
    private final short food;
    private final float foodSaturation;

    public PlayerHealthPacket(short health, short food, float foodSaturation) {
        this.health = health;
        this.food = food;
        this.foodSaturation = foodSaturation;
    }

    public short getHealth() {
        return this.health;
    }

    public short getFood() {
        return this.food;
    }

    public float getFoodSaturation() {
        return this.foodSaturation;
    }
}

