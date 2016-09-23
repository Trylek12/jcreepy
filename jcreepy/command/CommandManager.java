
package jcreepy.command;

import java.util.HashMap;
import java.util.Map;
import jcreepy.command.Command;

public final class CommandManager {
    private Map<String, Command> commandMap = new HashMap<String, Command>();

    public void registerCommand(String name, Command cmd) {
        if (!this.commandMap.containsKey(name)) {
            this.commandMap.put(name, cmd);
        }
    }

    public Command getCommand(String name) {
        return this.commandMap.get(name);
    }
}

