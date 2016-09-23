
package jcreepy.protocol.codec.auth;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.auth.EncKeyRequestPacket;
import jcreepy.protocol.util.ByteBufUtils;

public final class EncKeyRequestCodec
extends PacketCodec<EncKeyRequestPacket> {
    public EncKeyRequestCodec() {
        super(EncKeyRequestPacket.class, 253);
    }

    @Override
    public EncKeyRequestPacket decode(ByteBuf buffer) {
        String sessionId = ByteBufUtils.readString(buffer);
        int publicKeyLength = buffer.readUnsignedShort();
        byte[] publicKey = new byte[publicKeyLength];
        buffer.readBytes(publicKey);
        int tokenLength = buffer.readUnsignedShort();
        byte[] token = new byte[tokenLength];
        buffer.readBytes(token);
        return new EncKeyRequestPacket(sessionId, publicKey, token);
    }

    @Override
    public ByteBuf encode(EncKeyRequestPacket packet) {
        ByteBuf buffer = Unpooled.buffer();
        ByteBufUtils.writeString(buffer, packet.getSessionId());
        byte[] publicKey = packet.getRawPublicKey();
        buffer.writeShort((short)publicKey.length);
        buffer.writeBytes(publicKey);
        buffer.writeShort((short)packet.getVerifyToken().length);
        buffer.writeBytes(packet.getVerifyToken());
        return buffer;
    }
}

