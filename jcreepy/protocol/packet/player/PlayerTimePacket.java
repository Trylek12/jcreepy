
package jcreepy.protocol.packet.player;

import jcreepy.network.Packet;

public final class PlayerTimePacket
extends Packet {
    private final long age;
    private final long time;

    public PlayerTimePacket(long age, long time) {
        this.age = age;
        this.time = time;
    }

    public long getAge() {
        return this.age;
    }

    public long getTime() {
        return this.time;
    }
}

