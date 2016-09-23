
package jcreepy.protocol.packet.window;

import jcreepy.protocol.packet.window.WindowPacket;

public final class WindowPropertyPacket
extends WindowPacket {
    private final int progressBar;
    private final int value;

    public WindowPropertyPacket(int windowInstanceId, int progressBar, int value) {
        super(windowInstanceId);
        this.progressBar = progressBar;
        this.value = value;
    }

    public int getProgressBar() {
        return this.progressBar;
    }

    public int getValue() {
        return this.value;
    }
}

