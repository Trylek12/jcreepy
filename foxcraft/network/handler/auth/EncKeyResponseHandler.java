
package foxcraft.network.handler.auth;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import jcreepy.network.Packet;
import jcreepy.network.Session;
import jcreepy.network.handler.PacketHandler;
import jcreepy.network.stream.CipherCodec;
import jcreepy.protocol.packet.auth.EncKeyResponsePacket;
import jcreepy.protocol.packet.player.PlayerStatusPacket;
import jcreepy.protocol.util.CryptoUtil;

public class EncKeyResponseHandler
extends PacketHandler<EncKeyResponsePacket> {
    @Override
    public void handlePacket(EncKeyResponsePacket pk, Session session) throws Exception {
        if (pk.isEmpty()) {
            SecretKey key = session.getEncryptionKey();
            if (!session.isEncrypted() && key != null) {
                Cipher encrypt = CryptoUtil.getAESCipher(true, key);
                Cipher decrypt = CryptoUtil.getAESCipher(false, key);
                session.getChannel().pipeline().addBefore("decoder", "cipher", new CipherCodec(encrypt, decrypt));
                session.setEncrypted(true);
                session.send(new PlayerStatusPacket(0));
            }
        }
    }
}

