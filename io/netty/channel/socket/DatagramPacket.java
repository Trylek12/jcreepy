
package io.netty.channel.socket;

import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;

public class DatagramPacket {
    private final ByteBuf data;
    private final InetSocketAddress remoteAddress;

    public DatagramPacket(ByteBuf data, InetSocketAddress remoteAddress) {
        if (data == null) {
            throw new NullPointerException("data");
        }
        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress");
        }
        this.data = data;
        this.remoteAddress = remoteAddress;
    }

    public ByteBuf data() {
        return this.data;
    }

    public InetSocketAddress remoteAddress() {
        return this.remoteAddress;
    }

    public String toString() {
        return "datagram(" + this.data.readableBytes() + "B, " + this.remoteAddress + ')';
    }
}

