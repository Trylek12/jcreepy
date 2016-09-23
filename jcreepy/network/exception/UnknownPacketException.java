
package jcreepy.network.exception;

import java.io.IOException;

public class UnknownPacketException
extends IOException {
    private final int opcode;

    public UnknownPacketException(int opcode) {
        super("Unknown opcode: " + opcode);
        this.opcode = opcode;
    }

    public int getOpcode() {
        return this.opcode;
    }
}

