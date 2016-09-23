
package jcreepy.protocol.codec.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.server.ServerPluginPacket;
import jcreepy.protocol.util.ByteBufUtils;

public class ServerPluginCodec
extends PacketCodec<ServerPluginPacket> {
    public ServerPluginCodec() {
        super(ServerPluginPacket.class, 250);
    }

    @Override
    public ServerPluginPacket decode(ByteBuf buffer) throws IOException {
        String type = ByteBufUtils.readString(buffer);
        int length = buffer.readUnsignedShort();
        byte[] data = new byte[length];
        buffer.readBytes(data);
        return new ServerPluginPacket(type, data);
    }

    @Override
    public ByteBuf encode(ServerPluginPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        ByteBufUtils.writeString(buffer, packet.getType());
        buffer.writeShort(packet.getData().length);
        buffer.writeBytes(packet.getData());
        return buffer;
    }
}

