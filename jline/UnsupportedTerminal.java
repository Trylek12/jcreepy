
package jline;

import jline.TerminalSupport;

public class UnsupportedTerminal
extends TerminalSupport {
    public UnsupportedTerminal() {
        super(false);
        this.setAnsiSupported(false);
        this.setEchoEnabled(true);
    }
}

