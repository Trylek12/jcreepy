
package jcreepy.protocol.packet.window;

import jcreepy.protocol.packet.window.WindowPacket;

public final class WindowClosePacket
extends WindowPacket {
    public WindowClosePacket(int windowInstanceId) {
        super(windowInstanceId);
    }
}

