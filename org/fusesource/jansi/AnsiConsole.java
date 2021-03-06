
package org.fusesource.jansi;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import org.fusesource.jansi.AnsiOutputStream;
import org.fusesource.jansi.WindowsAnsiOutputStream;
import org.fusesource.jansi.internal.CLibrary;

public class AnsiConsole {
    public static final PrintStream system_out = System.out;
    public static final PrintStream out = new PrintStream(AnsiConsole.wrapOutputStream(system_out));
    public static final PrintStream system_err = System.err;
    public static final PrintStream err = new PrintStream(AnsiConsole.wrapOutputStream(system_err));
    private static int installed;

    public static OutputStream wrapOutputStream(OutputStream stream) {
        if (Boolean.getBoolean("jansi.passthrough")) {
            return stream;
        }
        if (Boolean.getBoolean("jansi.strip")) {
            return new AnsiOutputStream(stream);
        }
        String os = System.getProperty("os.name");
        if (os.startsWith("Windows")) {
            try {
                return new WindowsAnsiOutputStream(stream);
            }
            catch (Throwable ignore) {
                return new AnsiOutputStream(stream);
            }
        }
        try {
            int rc = CLibrary.isatty(CLibrary.STDOUT_FILENO);
            if (rc == 0) {
                return new AnsiOutputStream(stream);
            }
        }
        catch (NoClassDefFoundError ignore) {
        }
        catch (UnsatisfiedLinkError ignore) {
            // empty catch block
        }
        return new FilterOutputStream(stream){

            public void close() throws IOException {
                this.write(AnsiOutputStream.REST_CODE);
                this.flush();
                super.close();
            }
        };
    }

    public static PrintStream out() {
        return out;
    }

    public static PrintStream err() {
        return err;
    }

    public static synchronized void systemInstall() {
        if (++installed == 1) {
            System.setOut(out);
            System.setErr(err);
        }
    }

    public static synchronized void systemUninstall() {
        if (--installed == 0) {
            System.setOut(system_out);
            System.setErr(system_err);
        }
    }

}

