
package jcreepy.protocol.codec.window;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.window.WindowPropertyPacket;

public final class WindowPropertyCodec
extends PacketCodec<WindowPropertyPacket> {
    public WindowPropertyCodec() {
        super(WindowPropertyPacket.class, 105);
    }

    @Override
    public WindowPropertyPacket decode(ByteBuf buffer) throws IOException {
        short id = buffer.readUnsignedByte();
        int progressBar = buffer.readUnsignedShort();
        int value = buffer.readUnsignedShort();
        return new WindowPropertyPacket(id, progressBar, value);
    }

    @Override
    public ByteBuf encode(WindowPropertyPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(5);
        buffer.writeByte(packet.getWindowInstanceId());
        buffer.writeShort(packet.getProgressBar());
        buffer.writeShort(packet.getValue());
        return buffer;
    }
}

