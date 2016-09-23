
package jcreepy.protocol.codec.auth;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.auth.EncKeyResponsePacket;

public class EncKeyResponseCodec
extends PacketCodec<EncKeyResponsePacket> {
    public EncKeyResponseCodec() {
        super(EncKeyResponsePacket.class, 252);
    }

    @Override
    public EncKeyResponsePacket decode(ByteBuf buffer) {
        int secretLength = buffer.readUnsignedShort();
        byte[] secret = new byte[secretLength];
        buffer.readBytes(secret);
        int validateTokenLength = buffer.readUnsignedShort();
        byte[] validateToken = new byte[validateTokenLength];
        buffer.readBytes(validateToken);
        return new EncKeyResponsePacket(secret, validateToken);
    }

    @Override
    public ByteBuf encode(EncKeyResponsePacket packet) {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeShort((short)packet.getSharedSecret().length);
        buffer.writeBytes(packet.getSharedSecret());
        buffer.writeShort((short)packet.getVerifyToken().length);
        buffer.writeBytes(packet.getVerifyToken());
        return buffer;
    }
}

