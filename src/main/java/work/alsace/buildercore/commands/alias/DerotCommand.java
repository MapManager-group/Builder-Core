package work.alsace.buildercore.commands.alias;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DerotCommand implements CommandExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Component error = MM.deserialize("<gray>正确指令:\n<white>//derot [轴线 X,Y,Z] [度数] <gray>- 快捷执行创世神deform指令");
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (work.alsace.buildercore.commands.CommandHelp.requested(sender, command, s, strings)) return true;
        if (strings.length == 2 && sender instanceof Player player) {
            if (!player.hasPermission("buildercore.aliases")) {
                player.sendMessage(error);
                return false;
            }

            int degrees;
            try {
                degrees = Integer.parseInt(strings[1]);
            } catch (Exception var8) {
                player.sendMessage(error);
                return false;
            }

            float radiansPerDegree = 0.0174533F;
            float radian = (float)degrees * radiansPerDegree;
            if (strings[0].equalsIgnoreCase("x")) {
                player.performCommand("/deform rotate(y,z," + radian + ")");
            } else if (strings[0].equalsIgnoreCase("y")) {
                player.performCommand("/deform rotate(x,z," + radian + ")");
            } else if (strings[0].equalsIgnoreCase("z")) {
                player.performCommand("/deform rotate(x,y," + radian + ")");
            } else {
                player.sendMessage(error);
                return false;
            }
            return true;
        }
        sender.sendMessage(error);
        return false;
    }
}
