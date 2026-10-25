package work.alsace.buildercore.commands.alias;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ScaleCommand implements CommandExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Component error = MM.deserialize("<gray>正确指令:\n<white>//scale [数值] <gray>- 快捷执行创世神deform指令");
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            if (!player.hasPermission("buildercore.aliases")) {
                player.sendMessage(MM.deserialize("<red>你没有使用该命令的权限"));
                return false;
            }

            double size;
            try {
                size = Double.parseDouble(args[0]);
            } catch (Exception e) {
                sender.sendMessage(error);
                return false;
            }

            player.performCommand("/deform x/=" + size + ";y/=" + size + ";z/=" + size);
            return true;
        }

        sender.sendMessage(error);
        return false;
    }
}
