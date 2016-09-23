
package foxcraft.command;

import foxcraft.CreepyClient;
import jcreepy.command.Command;
import jcreepy.command.CommandSender;

public class DisconnectCommand
extends Command {
    @Override
    public void execute(CommandSender sender, String[] args) {
        CreepyClient.getInstance().closeConnection();
    }
}

