/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  jcreepy.event.Event
 *  jcreepy.event.Listener
 *  jcreepy.event.events.ChatMessageEvent
 */
package foxcraft.listener;

import java.io.PrintStream;
import jcreepy.chat.ChatUtil;
import jcreepy.event.Event;
import jcreepy.event.Listener;
import jcreepy.event.events.ChatMessageEvent;
import org.fusesource.jansi.Ansi;

public class ChatMessageListener
implements Listener<ChatMessageEvent> {
    public void onEvent(ChatMessageEvent event) {
        String msg = event.getMessage();
        System.out.println(ChatUtil.messageToAnsi(msg) + Ansi.ansi().reset());
    }
}

