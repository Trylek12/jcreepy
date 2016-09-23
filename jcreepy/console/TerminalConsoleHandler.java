
package jcreepy.console;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import jcreepy.console.TerminalHandler;
import jline.console.ConsoleReader;
import jline.console.CursorBuffer;

public class TerminalConsoleHandler
extends ConsoleHandler {
    private final TerminalHandler term;
    private final ConsoleReader reader;

    public TerminalConsoleHandler(TerminalHandler term) {
        this.term = term;
        this.reader = term.getConsoleReader();
    }

    @Override
    public synchronized void flush() {
        block5 : {
            try {
                if (this.term.isUsingJLine()) {
                    this.reader.print("\r");
                    this.reader.flush();
                    super.flush();
                    try {
                        this.reader.drawLine();
                    }
                    catch (Throwable ex) {
                        this.reader.getCursorBuffer().clear();
                    }
                    this.reader.flush();
                    break block5;
                }
                super.flush();
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}

