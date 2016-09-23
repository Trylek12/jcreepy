
package jcreepy.protocol.packet.server;

import jcreepy.network.Packet;

public final class ServerPluginPacket
extends Packet {
    private final byte[] data;
    private final String type;

    public ServerPluginPacket(String type, byte[] data) {
        this.type = type;
        this.data = data;
    }

    public String getType() {
        return this.type;
    }

    public byte[] getData() {
        return this.data;
    }
}

