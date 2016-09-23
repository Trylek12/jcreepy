
package jcreepy.protocol.codec.window;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.inventory.ItemStack;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.window.WindowCreativeActionPacket;
import jcreepy.protocol.util.ByteBufUtils;

public class WindowCreativeActionCodec
extends PacketCodec<WindowCreativeActionPacket> {
    public WindowCreativeActionCodec() {
        super(WindowCreativeActionPacket.class, 107);
    }

    @Override
    public WindowCreativeActionPacket decode(ByteBuf buffer) throws IOException {
        short slot = buffer.readShort();
        ItemStack item = ByteBufUtils.readItemStack(buffer);
        return new WindowCreativeActionPacket(slot, item);
    }

    @Override
    public ByteBuf encode(WindowCreativeActionPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeShort(packet.getSlot());
        ByteBufUtils.writeItemStack(buffer, packet.get());
        return buffer;
    }
}

