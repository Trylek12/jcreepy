
package jcreepy.protocol.codec.window;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.inventory.ItemStack;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.window.WindowItemsPacket;
import jcreepy.protocol.util.ByteBufUtils;

public final class WindowItemsCodec
extends PacketCodec<WindowItemsPacket> {
    public WindowItemsCodec() {
        super(WindowItemsPacket.class, 104);
    }

    @Override
    public WindowItemsPacket decode(ByteBuf buffer) throws IOException {
        byte id = buffer.readByte();
        int count = buffer.readShort();
        ItemStack[] items = new ItemStack[count];
        for (int slot = 0; slot < count; ++slot) {
            items[slot] = ByteBufUtils.readItemStack(buffer);
        }
        return new WindowItemsPacket(id, items);
    }

    @Override
    public ByteBuf encode(WindowItemsPacket packet) throws IOException {
        ItemStack[] items = packet.getItems();
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(packet.getWindowInstanceId());
        buffer.writeShort(items.length);
        for (ItemStack item : items) {
            ByteBufUtils.writeItemStack(buffer, item);
        }
        return buffer;
    }
}

