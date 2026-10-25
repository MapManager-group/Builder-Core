package work.alsace.buildercore.commands.time;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PTimeCommand implements TabExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Component error = MM.deserialize("<gray>正确指令:\n<white>/ptime <时间> <gray>- 设置你自己的客户端时间\n<white>/ptime <时间> <玩家> <gray>- 设置指定玩家的客户端时间");
    private static final List<String> times = Arrays.asList("day", "night", "sunrise", "morning", "noon", "afternoon", "sunset", "midnight", "reset");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (strings.length != 1 && strings.length != 2) {
            sender.sendMessage(error);
            return false;
        }

        if (!(sender instanceof Player) && strings.length == 1) {
            sender.sendMessage(MM.deserialize("<red>控制台无法执行该命令"));
            return false;
        }

        Player targetPlayer;
        if (strings.length == 2) {
            if (!sender.hasPermission("buildercore.commands.ptime.other")) {
                sender.sendMessage(MM.deserialize("<red>你没有使用该命令的权限"));
                return false;
            }

            targetPlayer = Bukkit.getPlayer(strings[1]);
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                sender.sendMessage(MM.deserialize("<red>指定的玩家不在线或不存在"));
                return false;
            }
        } else {
            targetPlayer = (Player) sender;
            if (!targetPlayer.hasPermission("buildercore.commands.ptime")) {
                targetPlayer.sendMessage(MM.deserialize("<red>你没有使用该命令的权限"));
                return false;
            }
        }

        if (strings[0].equalsIgnoreCase("reset")) {
            targetPlayer.resetPlayerTime();
            targetPlayer.sendMessage(MM.deserialize("<gray>已将你的客户端时间重置"));
            if (strings.length == 2) {
                sender.sendMessage(MM.deserialize("<gray>已将玩家 <white>" + targetPlayer.getName() + " <gray>的客户端时间重置"));
            }
            return true;
        }
        long time = TimeCommand.getTimeFromKeyword(strings[0]);
        if (time == -1) {
            sender.sendMessage(error);
            return false;
        }

        targetPlayer.setPlayerTime(time, false);
        targetPlayer.sendMessage(MM.deserialize("<gray>已将你的客户端时间设置为 <white>" + time + " ticks"));

        if (strings.length == 2) {
            sender.sendMessage(MM.deserialize("<gray>已将玩家 <white>" + targetPlayer.getName() + " <gray>的客户端时间设置为 <white>" + time + " ticks"));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] strings) {
        if (strings.length == 1 && sender.hasPermission("buildercore.commands.ptime")) {
            return times;
        }
        if (strings.length == 2 && sender.hasPermission("buildercore.commands.ptime.other")) {
            List<String> list = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                list.add(p.getName());
            }
            return list;
        }
        return Collections.emptyList();
    }
}
