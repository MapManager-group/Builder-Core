package work.alsace.buildercore.commands.time;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TimeCommand implements TabExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Component error = MM.deserialize("<gray>正确指令:\n<white>/time <时间> <gray>- 设置你当前世界的时间\n<white>/time set|add <数值> <gray>- 设置或增加当前世界的时间");
    private static final List<String> times1 = Arrays.asList("sunrise", "day", "morning", "noon", "afternoon", "sunset", "night", "midnight", "5000");

    private static final List<String> times0 = Arrays.asList("sunrise", "day", "morning", "noon", "afternoon", "sunset", "night", "midnight", "5000", "set", "add");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MM.deserialize("<red>该命令仅限玩家执行"));
            return false;
        }
        if (!player.hasPermission("buildercore.commands.time")) {
            player.sendMessage(MM.deserialize("<red>你没有使用该命令的权限"));
            return false;
        }
        if (strings.length == 1) {
            World world = player.getWorld();
            long timeTicks = getTimeFromKeyword(strings[0]);
            if (timeTicks == -1) {
                player.sendMessage(error);
                return false;
            }
            world.setTime(timeTicks);
            player.sendMessage(MM.deserialize("<gray>已将当前世界时间设置为 <white>" + timeTicks + " ticks"));
            return true;
        }

        if (strings.length == 2) {
            switch (strings[0]) {
                case "set" -> {
                    long timeTicks = getTimeFromKeyword(strings[1]);
                    if (timeTicks == -1) {
                        sender.sendMessage(error);
                        return false;
                    }
                    player.getWorld().setTime(timeTicks);
                    player.sendMessage(MM.deserialize("<gray>已将当前世界时间设置为 <white>" + strings[1] + " ticks"));
                    return true;
                }
                case "add" -> {
                    if (!StringUtils.isNumeric(strings[1])) {
                        sender.sendMessage(error);
                        return false;
                    }
                    try {
                        long amount = Long.parseLong(strings[1]);
                        player.getWorld().setTime(Math.addExact(player.getWorld().getTime(), amount));
                        player.sendMessage(MM.deserialize("<gray>已将当前世界时间增加 <white>" + amount + " ticks"));
                    } catch (NumberFormatException | ArithmeticException exception) {
                        player.sendMessage(error);
                        return false;
                    }
                    return true;
                }
            }
        }
        sender.sendMessage(error);
        return false;
    }

    static long getTimeFromKeyword(String keyword) {
        switch (keyword.toLowerCase()) {
            case "sunrise" -> {
                return 21000;
            }
            case "day" -> {
                return 0;
            }
            case "morning" -> {
                return 3000;
            }
            case "noon" -> {
                return 6000;
            }
            case "afternoon" -> {
                return 9000;
            }
            case "sunset" -> {
                return 12000;
            }
            case "night" -> {
                return 15000;
            }
            case "midnight" -> {
                return 18000;
            }
            default -> {
                try {
                    return Long.parseLong(keyword);
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player) || !sender.hasPermission("buildercore.commands.time")) {
            return Collections.emptyList();
        }
        if (strings.length == 1) {
            return times0;
        }
        if (strings.length == 2 && strings[0].equals("set")) {
            return times1;
        }
        if (strings.length == 2 && strings[0].equals("add")) {
            return List.of("1000");
        }
        return Collections.emptyList();
    }
}
