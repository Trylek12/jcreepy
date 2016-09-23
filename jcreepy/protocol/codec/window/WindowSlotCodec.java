
package jcreepy.protocol.codec.window;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.inventory.ItemStack;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.window.WindowSlotPacket;
import jcreepy.protocol.util.ByteBufUtils;

public final class WindowSlotCodec
extends PacketCodec<WindowSlotPacket> {
    public WindowSlotCodec() {
        super(WindowSlotPacket.class, 103);
    }

    @Override
    public WindowSlotPacket decode(ByteBuf buffer) throws IOException {
        byte id = buffer.readByte();
        short slot = buffer.readShort();
        ItemStack item = ByteBufUtils.readItemStack(buffer);
        return new WindowSlotPacket(id, slot, item);
    }

    @Override
    public ByteBuf encode(WindowSlotPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(packet.getWindowInstanceId());
        buffer.writeShort(packet.getSlot());
        ByteBufUtils.writeItemStack(buffer, packet.get());
        return buffer;
    }
}

