/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  jcreepy.event.Event
 *  jcreepy.event.EventManager
 *  jcreepy.event.events.PlayerHealthEvent
 */
package foxcraft.network.handler.player;

import foxcraft.CreepyClient;
import jcreepy.event.Event;
import jcreepy.event.EventManager;
import jcreepy.event.events.PlayerHealthEvent;
import jcreepy.network.Packet;
import jcreepy.network.Session;
import jcreepy.network.handler.PacketHandler;
import jcreepy.protocol.packet.player.PlayerHealthPacket;

public class PlayerHealthHandler
extends PacketHandler<PlayerHealthPacket> {
    @Override
    public void handlePacket(PlayerHealthPacket packet, Session session) {
        CreepyClient.getInstance().getEventManager().callEvent((Event)new PlayerHealthEvent(packet));
    }
}

