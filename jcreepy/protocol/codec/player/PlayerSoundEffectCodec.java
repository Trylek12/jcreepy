
package jcreepy.protocol.codec.player;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.PlayerSoundEffectPacket;
import jcreepy.protocol.util.ByteBufUtils;

public final class PlayerSoundEffectCodec
extends PacketCodec<PlayerSoundEffectPacket> {
    public PlayerSoundEffectCodec() {
        super(PlayerSoundEffectPacket.class, 62);
    }

    @Override
    public PlayerSoundEffectPacket decode(ByteBuf buffer) throws IOException {
        String soundName = ByteBufUtils.readString(buffer);
        float x = (float)buffer.readInt() / 8.0f;
        float y = (float)buffer.readInt() / 8.0f;
        float z = (float)buffer.readInt() / 8.0f;
        float volume = buffer.readFloat();
        float pitch = 63.0f / (float)buffer.readUnsignedByte();
        return new PlayerSoundEffectPacket(soundName, x, y, z, volume, pitch);
    }

    @Override
    public ByteBuf encode(PlayerSoundEffectPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        ByteBufUtils.writeString(buffer, packet.getSoundName());
        buffer.writeInt((int)(packet.getX() * 8.0f));
        buffer.writeInt((int)(packet.getY() * 8.0f));
        buffer.writeInt((int)(packet.getZ() * 8.0f));
        buffer.writeFloat(packet.getVolume());
        buffer.writeByte((byte)(packet.getPitch() * 63.0f));
        return buffer;
    }
}

