
package jline;

import java.io.PrintStream;
import jline.TerminalSupport;
import jline.internal.Log;
import jline.internal.TerminalLineSettings;

public class UnixTerminal
extends TerminalSupport {
    private final TerminalLineSettings settings = new TerminalLineSettings();

    public UnixTerminal() throws Exception {
        super(true);
    }

    protected TerminalLineSettings getSettings() {
        return this.settings;
    }

    public void init() throws Exception {
        super.init();
        this.setAnsiSupported(true);
        this.settings.set("-icanon min 1 -icrnl -inlcr");
        this.setEchoEnabled(false);
    }

    public void restore() throws Exception {
        this.settings.restore();
        super.restore();
        System.out.println();
    }

    public int getWidth() {
        int w = this.settings.getProperty("columns");
        return w < 1 ? 80 : w;
    }

    public int getHeight() {
        int h = this.settings.getProperty("rows");
        return h < 1 ? 24 : h;
    }

    public synchronized void setEchoEnabled(boolean enabled) {
        try {
            if (enabled) {
                this.settings.set("echo");
            } else {
                this.settings.set("-echo");
            }
            super.setEchoEnabled(enabled);
        }
        catch (Exception e) {
            Object[] arrobject = new Object[4];
            arrobject[0] = "Failed to ";
            arrobject[1] = enabled ? "enable" : "disable";
            arrobject[2] = " echo";
            arrobject[3] = e;
            Log.error(arrobject);
        }
    }
}

