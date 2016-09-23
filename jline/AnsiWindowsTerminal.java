
package jline;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import jline.WindowsTerminal;
import jline.internal.Configuration;
import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.AnsiOutputStream;
import org.fusesource.jansi.WindowsAnsiOutputStream;

public class AnsiWindowsTerminal
extends WindowsTerminal {
    private final boolean ansiSupported = AnsiWindowsTerminal.detectAnsiSupport();

    public OutputStream wrapOutIfNeeded(OutputStream out) {
        return AnsiWindowsTerminal.wrapOutputStream(out);
    }

    private static OutputStream wrapOutputStream(OutputStream stream) {
        if (Configuration.isWindows()) {
            try {
                return new WindowsAnsiOutputStream(stream);
            }
            catch (Throwable ignore) {
                return new AnsiOutputStream(stream);
            }
        }
        return stream;
    }

    private static boolean detectAnsiSupport() {
        OutputStream out = AnsiConsole.wrapOutputStream(new ByteArrayOutputStream());
        try {
            out.close();
        }
        catch (Exception e) {
            // empty catch block
        }
        return out instanceof WindowsAnsiOutputStream;
    }

    public boolean isAnsiSupported() {
        return this.ansiSupported;
    }

    public boolean hasWeirdWrap() {
        return false;
    }
}

