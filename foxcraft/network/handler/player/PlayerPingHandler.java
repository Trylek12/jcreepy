
package foxcraft.network.handler.player;

import jcreepy.network.Packet;
import jcreepy.network.Session;
import jcreepy.network.handler.PacketHandler;
import jcreepy.protocol.packet.player.conn.PlayerPingPacket;

public class PlayerPingHandler
extends PacketHandler<PlayerPingPacket> {
    @Override
    public void handlePacket(PlayerPingPacket pk, Session session) {
        session.send(pk);
    }
}

