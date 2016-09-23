
package jcreepy.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import javax.crypto.SecretKey;
import jcreepy.network.Packet;
import jcreepy.network.Protocol;

public final class Session {
    private Channel channel = null;
    private SecretKey encryptionKey;
    private boolean encrypted;
    private String playerName;
    private String host = "localhost";
    private int port = 25565;
    private boolean active;
    private final Protocol protocol;

    public Session(Protocol protocol, String playerName) {
        this.protocol = protocol;
        this.playerName = playerName;
    }

    public Session send(Packet packet) {
        if (this.channel == null || !this.channel.isOpen()) {
            throw new IllegalStateException();
        }
        this.channel.write(packet);
        return this;
    }

    public /* varargs */ void sendAll(Packet ... packets) {
        for (Packet pk : packets) {
            this.send(pk);
        }
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Channel getChannel() {
        return this.channel;
    }

    public void setEncryptionKey(SecretKey key) {
        this.encryptionKey = key;
    }

    public SecretKey getEncryptionKey() {
        return this.encryptionKey;
    }

    public boolean isEncrypted() {
        return this.encrypted;
    }

    public void setEncrypted(boolean v) {
        this.encrypted = v;
    }

    public String getPlayerName() {
        return this.playerName;
    }

    public void setPlayerName(String v) {
        this.playerName = v;
    }

    public Protocol getProtocol() {
        return this.protocol;
    }

    public String getHost() {
        return this.host;
    }

    public void setHost(String v) {
        this.host = v;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int v) {
        this.port = v;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean v) {
        this.active = v;
    }
}

