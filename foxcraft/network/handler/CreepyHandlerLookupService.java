
package foxcraft.network.handler;

import foxcraft.network.handler.auth.EncKeyRequestHandler;
import foxcraft.network.handler.auth.EncKeyResponseHandler;
import foxcraft.network.handler.player.PlayerChatHandler;
import foxcraft.network.handler.player.PlayerPingHandler;
import jcreepy.network.Packet;
import jcreepy.network.handler.HandlerLookupService;
import jcreepy.network.handler.PacketHandler;
import jcreepy.protocol.packet.auth.EncKeyRequestPacket;
import jcreepy.protocol.packet.auth.EncKeyResponsePacket;
import jcreepy.protocol.packet.player.PlayerChatPacket;
import jcreepy.protocol.packet.player.conn.PlayerPingPacket;

public class CreepyHandlerLookupService
extends HandlerLookupService {
    public static final CreepyHandlerLookupService INSTANCE = new CreepyHandlerLookupService();

    public CreepyHandlerLookupService() {
        try {
            this.bind(EncKeyRequestPacket.class, EncKeyRequestHandler.class);
            this.bind(EncKeyResponsePacket.class, EncKeyResponseHandler.class);
            this.bind(PlayerPingPacket.class, PlayerPingHandler.class);
            this.bind(PlayerChatPacket.class, PlayerChatHandler.class);
        }
        catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
}

