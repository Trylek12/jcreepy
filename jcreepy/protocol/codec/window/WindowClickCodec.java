
package jcreepy.protocol.codec.window;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.inventory.ItemStack;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.window.WindowClickPacket;
import jcreepy.protocol.util.ByteBufUtils;

public final class WindowClickCodec
extends PacketCodec<WindowClickPacket> {
    public WindowClickCodec() {
        super(WindowClickPacket.class, 102);
    }

    @Override
    public WindowClickPacket decode(ByteBuf buffer) throws IOException {
        short id = buffer.readUnsignedByte();
        int slot = buffer.readUnsignedShort();
        boolean rightClick = buffer.readUnsignedByte() != 0;
        int transaction = buffer.readUnsignedShort();
        boolean shift = buffer.readUnsignedByte() != 0;
        ItemStack item = ByteBufUtils.readItemStack(buffer);
        return new WindowClickPacket(id, slot, rightClick, transaction, shift, item);
    }

    @Override
    public ByteBuf encode(WindowClickPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(packet.getWindowInstanceId());
        buffer.writeShort(packet.getSlot());
        buffer.writeByte(packet.isRightClick() ? 1 : 0);
        buffer.writeShort(packet.getTransaction());
        buffer.writeByte(packet.isShift() ? 1 : 0);
        ByteBufUtils.writeItemStack(buffer, packet.get());
        return buffer;
    }
}

