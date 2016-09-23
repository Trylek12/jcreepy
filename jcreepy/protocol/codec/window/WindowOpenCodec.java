
package jcreepy.protocol.codec.window;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.window.WindowOpenPacket;
import jcreepy.protocol.util.ByteBufUtils;

public final class WindowOpenCodec
extends PacketCodec<WindowOpenPacket> {
    public WindowOpenCodec() {
        super(WindowOpenPacket.class, 100);
    }

    @Override
    public WindowOpenPacket decode(ByteBuf buffer) throws IOException {
        short id = buffer.readUnsignedByte();
        short windowType = buffer.readUnsignedByte();
        String title = ByteBufUtils.readString(buffer);
        short slots = buffer.readUnsignedByte();
        return new WindowOpenPacket(id, windowType, title, slots);
    }

    @Override
    public ByteBuf encode(WindowOpenPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(packet.getWindowInstanceId());
        buffer.writeByte(packet.getType());
        ByteBufUtils.writeString(buffer, packet.getTitle());
        buffer.writeByte(packet.getSlots());
        return buffer;
    }
}

