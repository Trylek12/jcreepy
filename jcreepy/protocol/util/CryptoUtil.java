
package jcreepy.protocol.util;

import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public final class CryptoUtil {
    public static Cipher getAESCipher(boolean forEncryption, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
        IvParameterSpec ivSpec = new IvParameterSpec(key.getEncoded());
        int opmode = forEncryption ? 1 : 2;
        cipher.init(opmode, (Key)key, ivSpec);
        return cipher;
    }

    private CryptoUtil() {
    }
}

