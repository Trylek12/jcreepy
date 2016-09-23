
package jcreepy.protocol.packet.window;

import jcreepy.network.Packet;

public abstract class WindowPacket
extends Packet {
    private final int instanceId;

    public WindowPacket(int instanceId) {
        this.instanceId = instanceId;
    }

    public int getWindowInstanceId() {
        return this.instanceId;
    }
}

