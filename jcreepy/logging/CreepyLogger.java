
package jcreepy.logging;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import jcreepy.console.TerminalConsoleHandler;
import jcreepy.console.TerminalHandler;
import jcreepy.logging.ConsoleLogFormatter;
import jcreepy.logging.LoggerOutputStream;

public class CreepyLogger
extends Logger {
    public CreepyLogger(TerminalHandler term) {
        super("", null);
        TerminalConsoleHandler tch = new TerminalConsoleHandler(term);
        tch.setFormatter(new ConsoleLogFormatter());
        this.addHandler(tch);
        System.setOut(new PrintStream(new LoggerOutputStream(this, Level.INFO), true));
        System.setErr(new PrintStream(new LoggerOutputStream(this, Level.SEVERE), true));
    }
}

