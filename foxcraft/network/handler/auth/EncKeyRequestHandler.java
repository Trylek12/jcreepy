
package foxcraft.network.handler.auth;

import java.security.Key;
import java.security.PublicKey;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import jcreepy.network.Packet;
import jcreepy.network.Session;
import jcreepy.network.handler.PacketHandler;
import jcreepy.protocol.packet.auth.EncKeyRequestPacket;
import jcreepy.protocol.packet.auth.EncKeyResponsePacket;

public class EncKeyRequestHandler
extends PacketHandler<EncKeyRequestPacket> {
    @Override
    public void handlePacket(EncKeyRequestPacket pk, Session session) throws Exception {
        Cipher c = Cipher.getInstance("RSA");
        SecretKeySpec key = new SecretKeySpec(new byte[16], "AES");
        c.init(1, pk.getPublicKey());
        session.setEncryptionKey(key);
        EncKeyResponsePacket p = new EncKeyResponsePacket(key.getEncoded(), pk.getVerifyToken()).crypt(c);
        session.send(p);
    }
}

