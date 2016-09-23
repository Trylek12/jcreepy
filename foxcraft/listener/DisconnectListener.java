/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  jcreepy.event.Event
 *  jcreepy.event.Listener
 *  jcreepy.event.events.DisconnectEvent
 */
package foxcraft.listener;

import foxcraft.CreepyClient;
import java.io.PrintStream;
import jcreepy.event.Event;
import jcreepy.event.Listener;
import jcreepy.event.events.DisconnectEvent;
import jcreepy.network.Session;

public class DisconnectListener
implements Listener<DisconnectEvent> {
    public void onEvent(DisconnectEvent event) {
        CreepyClient.getInstance().getSession().setActive(false);
        System.out.println("Disconnected from server!");
    }
}

