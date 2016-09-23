
package jcreepy.protocol.codec.window;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.window.WindowClosePacket;

public final class WindowCloseCodec
extends PacketCodec<WindowClosePacket> {
    public WindowCloseCodec() {
        super(WindowClosePacket.class, 101);
    }

    @Override
    public WindowClosePacket decode(ByteBuf buffer) throws IOException {
        short id = buffer.readUnsignedByte();
        return new WindowClosePacket(id);
    }

    @Override
    public ByteBuf encode(WindowClosePacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(1);
        buffer.writeByte(packet.getWindowInstanceId());
        return buffer;
    }
}

