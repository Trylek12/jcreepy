
package jcreepy.protocol.packet.auth;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import jcreepy.network.Packet;

public final class EncKeyRequestPacket
extends Packet {
    private final String sessionId;
    private final byte[] pubKey;
    private final byte[] verifyToken;

    public EncKeyRequestPacket(String sessionId, byte[] pubKey, byte[] verifyToken) {
        this.sessionId = sessionId;
        this.pubKey = pubKey;
        this.verifyToken = verifyToken;
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public byte[] getRawPublicKey() {
        return this.pubKey;
    }

    public byte[] getVerifyToken() {
        return this.verifyToken;
    }

    public PublicKey getPublicKey() {
        try {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(this.pubKey);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return factory.generatePublic(spec);
        }
        catch (Exception e) {
            return null;
        }
    }
}

