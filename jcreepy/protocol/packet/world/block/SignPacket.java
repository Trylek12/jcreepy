
package jcreepy.protocol.packet.world.block;

import jcreepy.network.Packet;

public final class SignPacket
extends Packet {
    private final int x;
    private final int y;
    private final int z;
    private final String[] message;

    public SignPacket(int x, int y, int z, String[] message) {
        if (message.length != 4) {
            throw new IllegalArgumentException();
        }
        this.x = x;
        this.y = y;
        this.z = z;
        this.message = message;
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

    public String[] getMessage() {
        return this.message;
    }
}

