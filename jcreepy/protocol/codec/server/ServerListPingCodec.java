
package jcreepy.protocol.codec.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.server.ServerListPingPacket;

public class ServerListPingCodec
extends PacketCodec<ServerListPingPacket> {
    private static final ServerListPingPacket LIST_PING_MESSAGE = new ServerListPingPacket();

    public ServerListPingCodec() {
        super(ServerListPingPacket.class, 254);
    }

    @Override
    public ServerListPingPacket decode(ByteBuf buffer) throws IOException {
        return LIST_PING_MESSAGE;
    }

    @Override
    public ByteBuf encode(ServerListPingPacket packet) throws IOException {
        return Unpooled.EMPTY_BUFFER;
    }
}

