
package jcreepy.protocol.codec.player;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.inventory.ItemStack;
import jcreepy.math.Vector3;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.PlayerBlockPlacementPacket;
import jcreepy.protocol.util.ByteBufUtils;

public final class PlayerBlockPlacementCodec
extends PacketCodec<PlayerBlockPlacementPacket> {
    public PlayerBlockPlacementCodec() {
        super(PlayerBlockPlacementPacket.class, 15);
    }

    @Override
    public PlayerBlockPlacementPacket decode(ByteBuf buffer) throws IOException {
        int x = buffer.readInt();
        short y = buffer.readUnsignedByte();
        int z = buffer.readInt();
        short direction = buffer.readUnsignedByte();
        ItemStack heldItem = ByteBufUtils.readItemStack(buffer);
        float dx = (float)buffer.readUnsignedByte() / 16.0f;
        float dy = (float)buffer.readUnsignedByte() / 16.0f;
        float dz = (float)buffer.readUnsignedByte() / 16.0f;
        return new PlayerBlockPlacementPacket(x, y, z, direction, new Vector3(dx, dy, dz), heldItem);
    }

    @Override
    public ByteBuf encode(PlayerBlockPlacementPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(packet.getX());
        buffer.writeByte(packet.getY());
        buffer.writeInt(packet.getZ());
        buffer.writeByte(packet.getDirection());
        ByteBufUtils.writeItemStack(buffer, packet.getHeldItem());
        buffer.writeByte((int)(packet.getFace().getX() * 16.0f));
        buffer.writeByte((int)(packet.getFace().getY() * 16.0f));
        buffer.writeByte((int)(packet.getFace().getZ() * 16.0f));
        return buffer;
    }
}

