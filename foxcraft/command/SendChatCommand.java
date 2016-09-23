
package foxcraft.command;

import foxcraft.CreepyClient;
import jcreepy.command.Command;
import jcreepy.command.CommandSender;
import jcreepy.network.Packet;
import jcreepy.network.Session;
import jcreepy.protocol.packet.player.PlayerChatPacket;
import org.apache.commons.lang3.StringUtils;

public class SendChatCommand
extends Command {
    @Override
    public void execute(CommandSender sender, String[] args) {
        CreepyClient creepy = CreepyClient.getInstance();
        if (!creepy.getSession().isActive()) {
            sender.sendMessage("Available only when connected to server");
            return;
        }
        String msg = StringUtils.join((Object[])args, " ");
        CreepyClient.getInstance().getSession().send(new PlayerChatPacket(msg));
    }
}

