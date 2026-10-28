package work.alsace.buildercore.commands.alias;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ConvexCommand implements CommandExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Component error = MM.deserialize("<gray>正确指令:\n<white>//convex <gray>- 快捷执行创世神sel指令");

    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (work.alsace.buildercore.commands.CommandHelp.requested(sender, command, s, strings)) return true;
        if (sender instanceof Player player) {
            if (!player.hasPermission("buildercore.aliases")) {
                player.sendMessage(error);
                return false;
            }
            player.performCommand("/sel convex");
            return true;
        }
        sender.sendMessage(error);
        return false;
    }
}
