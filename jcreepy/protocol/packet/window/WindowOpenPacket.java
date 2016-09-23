
package jcreepy.protocol.packet.window;

import jcreepy.protocol.packet.window.WindowPacket;

public final class WindowOpenPacket
extends WindowPacket {
    private final int slots;
    private final int type;
    private final String title;

    public WindowOpenPacket(int windowInstanceId, int type, String title, int slots) {
        super(windowInstanceId);
        this.type = type;
        this.title = title;
        this.slots = slots;
    }

    public int getSlots() {
        return this.slots;
    }

    public int getType() {
        return this.type;
    }

    public String getTitle() {
        return this.title;
    }
}

