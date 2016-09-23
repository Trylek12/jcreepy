
package jcreepy.protocol.packet.auth;

import javax.crypto.Cipher;
import jcreepy.network.Packet;

public final class EncKeyResponsePacket
extends Packet {
    private final byte[] sharedSecret;
    private final byte[] verifyToken;

    public EncKeyResponsePacket(byte[] sharedSecret, byte[] verifyToken) {
        this.sharedSecret = sharedSecret;
        this.verifyToken = verifyToken;
    }

    public byte[] getSharedSecret() {
        return this.sharedSecret;
    }

    public byte[] getVerifyToken() {
        return this.verifyToken;
    }

    public boolean isEmpty() {
        return this.sharedSecret.length == 0;
    }

    public EncKeyResponsePacket crypt(Cipher c) {
        try {
            return new EncKeyResponsePacket(c.doFinal(this.sharedSecret), c.doFinal(this.verifyToken));
        }
        catch (Exception e) {
            return null;
        }
    }
}

