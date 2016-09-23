
package jcreepy.console;

import foxcraft.CreepyClient;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import jcreepy.console.TerminalHandler;
import jline.console.ConsoleReader;

public class CommandReaderThread
extends Thread {
    private final TerminalHandler term;
    private final ExecutorService ex = Executors.newSingleThreadExecutor();

    public CommandReaderThread(TerminalHandler term) {
        this.setDaemon(true);
        this.term = term;
    }

    @Override
    public void run() {
        while (CreepyClient.getInstance().isRunning()) {
            try {
                final String command = this.term.isUsingJLine() ? this.term.getConsoleReader().readLine(">") : this.term.getConsoleReader().readLine();
                if (command == null || command.trim().length() == 0) continue;
                this.ex.submit(new Runnable(){

                    @Override
                    public void run() {
                        CommandReaderThread.this.term.handleConsoleCommand(command);
                    }
                });
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

