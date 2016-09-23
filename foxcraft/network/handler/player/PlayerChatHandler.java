/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  jcreepy.event.Event
 *  jcreepy.event.EventManager
 *  jcreepy.event.events.ChatMessageEvent
 */
package foxcraft.network.handler.player;

import foxcraft.CreepyClient;
import jcreepy.event.Event;
import jcreepy.event.EventManager;
import jcreepy.event.events.ChatMessageEvent;
import jcreepy.network.Packet;
import jcreepy.network.Session;
import jcreepy.network.handler.PacketHandler;
import jcreepy.protocol.packet.player.PlayerChatPacket;

public class PlayerChatHandler
extends PacketHandler<PlayerChatPacket> {
    @Override
    public void handlePacket(PlayerChatPacket packet, Session session) {
        CreepyClient.getInstance().getEventManager().callEvent((Event)new ChatMessageEvent(packet.getMessage()));
    }
}

