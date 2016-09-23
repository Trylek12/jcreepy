
package jcreepy.protocol.packet.window;

import jcreepy.protocol.packet.window.WindowPacket;

public final class WindowTransactionPacket
extends WindowPacket {
    private final int transaction;
    private final boolean accepted;

    public WindowTransactionPacket(int windowInstanceId, int transaction, boolean accepted) {
        super(windowInstanceId);
        this.transaction = transaction;
        this.accepted = accepted;
    }

    public int getTransaction() {
        return this.transaction;
    }

    public boolean isAccepted() {
        return this.accepted;
    }
}

