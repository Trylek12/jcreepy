
package jcreepy.protocol.codec.player.conn;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.conn.PlayerHandshakePacket;
import jcreepy.protocol.util.ByteBufUtils;

public final class PlayerHandshakeCodec
extends PacketCodec<PlayerHandshakePacket> {
    public PlayerHandshakeCodec() {
        super(PlayerHandshakePacket.class, 2);
    }

    @Override
    public PlayerHandshakePacket decode(ByteBuf buffer) {
        byte protoVersion = buffer.readByte();
        String username = ByteBufUtils.readString(buffer);
        String hostname = ByteBufUtils.readString(buffer);
        int port = buffer.readInt();
        return new PlayerHandshakePacket(protoVersion, username, hostname, port);
    }

    @Override
    public ByteBuf encode(PlayerHandshakePacket packet) {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(packet.getProtocolVersion());
        ByteBufUtils.writeString(buffer, packet.getUsername());
        ByteBufUtils.writeString(buffer, packet.getHostname());
        buffer.writeInt(packet.getPort());
        return buffer;
    }
}

