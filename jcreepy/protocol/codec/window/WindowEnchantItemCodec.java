
package jcreepy.protocol.codec.window;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.window.WindowEnchantItemPacket;

public class WindowEnchantItemCodec
extends PacketCodec<WindowEnchantItemPacket> {
    public WindowEnchantItemCodec() {
        super(WindowEnchantItemPacket.class, 108);
    }

    @Override
    public WindowEnchantItemPacket decode(ByteBuf buffer) throws IOException {
        byte transaction = buffer.readByte();
        byte enchantment = buffer.readByte();
        return new WindowEnchantItemPacket(transaction, enchantment);
    }

    @Override
    public ByteBuf encode(WindowEnchantItemPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer(2);
        buffer.writeByte(packet.getTransaction());
        buffer.writeByte(packet.getEnchantment());
        return buffer;
    }
}

