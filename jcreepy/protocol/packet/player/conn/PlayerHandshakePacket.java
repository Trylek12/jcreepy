
package jcreepy.protocol.packet.player.conn;

import jcreepy.network.Packet;

public final class PlayerHandshakePacket
extends Packet {
    private final byte protoVersion;
    private final String username;
    private final String hostname;
    private final int port;

    public PlayerHandshakePacket(byte protoVersion, String username, String hostname, int port) {
        this.protoVersion = protoVersion;
        this.username = username;
        this.hostname = hostname;
        this.port = port;
    }

    public byte getProtocolVersion() {
        return this.protoVersion;
    }

    public String getUsername() {
        return this.username;
    }

    public String getHostname() {
        return this.hostname;
    }

    public int getPort() {
        return this.port;
    }
}

