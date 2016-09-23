
package jcreepy.console;

import java.io.PrintStream;
import jcreepy.command.CommandSender;

public class ConsoleCommandSender
implements CommandSender {
    @Override
    public void sendMessage(String msg) {
        System.out.println(msg);
    }

    @Override
    public String getName() {
        return "Console";
    }
}

