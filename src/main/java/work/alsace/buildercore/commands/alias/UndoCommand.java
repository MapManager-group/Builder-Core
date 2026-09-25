package work.alsace.buildercore.commands.alias;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UndoCommand implements CommandExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) {
            return false;
        } else if (!sender.hasPermission("buildercore.aliases")) {
            sender.sendMessage(MM.deserialize("<red>你没有使用该命令的权限"));
            return false;
        } else {
            ((Player) sender).performCommand("/undo");
            return true;
        }
    }
}
