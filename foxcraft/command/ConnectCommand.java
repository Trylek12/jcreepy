
package foxcraft.command;

import foxcraft.CreepyClient;
import jcreepy.command.Command;
import jcreepy.command.CommandSender;
import jcreepy.network.Session;

public class ConnectCommand
extends Command {
    private final CreepyClient client;

    public ConnectCommand(CreepyClient client) {
        this.client = client;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (this.client.getSession() != null && this.client.getSession().isActive()) {
            sender.sendMessage("Already connected to server!");
        } else if (args.length >= 3) {
            this.client.connect(args[0], args[1], Integer.parseInt(args[2]));
        }
    }
}

