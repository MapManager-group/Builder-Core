package work.alsace.buildercore.commands.players;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlyCommand implements CommandExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (strings.length == 0 && sender instanceof Player player) {
            if (!player.hasPermission("buildercore.commands.fly")) {
                player.sendMessage(MM.deserialize("<red>你没有使用该命令的权限"));
                return false;
            }
            if (player.getAllowFlight()) {
                player.setAllowFlight(false);
                player.sendMessage(MM.deserialize("<gray>已将你的飞行模式设置为 <red>关闭"));
            } else {
                player.setAllowFlight(true);
                player.sendMessage(MM.deserialize("<gray>已将你的飞行模式设置为 <green>开启"));
            }
            return true;
        }

        if (strings.length == 1) {
            if (!sender.hasPermission("buildercore.commands.fly.other")) {
                sender.sendMessage(MM.deserialize("<red>你没有使用该命令的权限"));
                return false;
            }
            Player player = Bukkit.getPlayer(strings[0]);
            if (player != null && player.isOnline()) {
                if (player.getAllowFlight()) {
                    player.setAllowFlight(false);
                    sender.sendMessage(MM.deserialize("<gray>已将玩家 <white>" + player.getName() + " <gray>的飞行模式设置为 <red>关闭"));
                    player.sendMessage(MM.deserialize("<gray>已将你的飞行模式设置为 <red>关闭"));
                } else {
                    player.setAllowFlight(true);
                    sender.sendMessage(MM.deserialize("<gray>已将玩家 <white>" + player.getName() + " <gray>的飞行模式设置为 <green>开启"));
                    player.sendMessage(MM.deserialize("<gray>已将你的飞行模式设置为 <green>开启"));
                }
                return true;
            } else {
                sender.sendMessage(MM.deserialize("<red>指定的玩家不在线或不存在"));
                return false;
            }
        }

        sender.sendMessage(MM.deserialize("<gray>正确指令:\n<white>/fly <gray>- 设置你的飞行状态\n<white>/fly <玩家> <gray>- 设置指定玩家的飞行状态"));
        return false;
    }
}
