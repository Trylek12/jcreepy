
package jcreepy.console;

import jline.console.ConsoleReader;

public interface TerminalHandler {
    public boolean isUsingJLine();

    public void handleConsoleCommand(String var1);

    public ConsoleReader getConsoleReader();
}

