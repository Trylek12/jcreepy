
package jline;

import jline.UnixTerminal;
import jline.internal.TerminalLineSettings;

public class NoInterruptUnixTerminal
extends UnixTerminal {
    public void init() throws Exception {
        super.init();
        this.getSettings().set("intr undef");
    }

    public void restore() throws Exception {
        this.getSettings().set("intr ^C");
        super.restore();
    }
}

