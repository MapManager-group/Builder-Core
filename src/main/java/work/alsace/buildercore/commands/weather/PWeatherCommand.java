package work.alsace.buildercore.commands.weather;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PWeatherCommand implements TabExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final Component error = MM.deserialize("<gray>正确指令:\n<white>/pweather <天气> <gray>- 设置你自己客户端的天气\n<white>/pweather <天气> <玩家> <gray>- 设置指定玩家的客户端天气");
    private static final List<String> weathers = Arrays.asList("clear", "rain", "reset");

    @Override
    public boolean onCommand(@NotNull CommandSender cmdSender, @NotNull Command cmd, @NotNull String s, String[] args) {
        if (work.alsace.buildercore.commands.CommandHelp.requested(cmdSender, cmd, s, args)) return true;
        if (!(cmdSender instanceof Player)) {
            cmdSender.sendMessage(MM.deserialize("<red>该命令只能由玩家执行"));
            return false;
        }

        if (!cmdSender.hasPermission("buildercore.commands.pweather")) {
            cmdSender.sendMessage(MM.deserialize("<red>你没有使用该命令的权限"));
            return false;
        }

        Player player = (Player) cmdSender;

        if (args.length == 0 || args.length > 2) {
            player.sendMessage(error);
            return false;
        }

        String weather = args[0].toLowerCase(java.util.Locale.ROOT);
        if (!weathers.contains(weather)) {
            player.sendMessage(MM.deserialize("<red>请输入有效的天气类型: clear, rain, reset"));
            return false;
        }

        if (args.length == 1) {
            return setWeather(player, weather);
        }

        if (!player.hasPermission("buildercore.commands.pweather.other")) {
            player.sendMessage(MM.deserialize("<red>你没有修改其他玩家客户端天气的权限"));
            return false;
        }
        Player targetPlayer = player.getServer().getPlayer(args[1]);
        if (targetPlayer == null) {
            player.sendMessage(MM.deserialize("<red>玩家 " + args[1] + " 不存在或不在线"));
            return true;
        }

        return setWeather(targetPlayer, weather);
    }

    private boolean setWeather(Player player, String weather) {
        switch (weather) {
            case "clear" -> {
                player.setPlayerWeather(org.bukkit.WeatherType.CLEAR);
                player.sendMessage(MM.deserialize("已将"+player.getName() +"的世界天气设置为 <white>晴天"));
                return true;
            }
            case "rain" -> {
                player.setPlayerWeather(org.bukkit.WeatherType.DOWNFALL);
                player.sendMessage(MM.deserialize("已将"+player.getName() +"的世界天气设置为 <white>雨天"));
                return true;
            }
            case "reset" -> {
                player.resetPlayerWeather();
                player.sendMessage(MM.deserialize("<gray>已将你的客户端天气恢复为世界天气"));
                return true;
            }
            default -> {
                player.sendMessage(error);
                return false;
            }
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender cmdSender, @NotNull Command cmd, @NotNull String s, String[] args) {
        if (args.length == 1 && cmdSender.hasPermission("buildercore.commands.pweather")) {
            return weathers;
        }
        if (args.length == 2 && cmdSender instanceof Player player
                && player.hasPermission("buildercore.commands.pweather")
                && player.hasPermission("buildercore.commands.pweather.other")) {
            return player.getServer().getOnlinePlayers().stream().filter(player::canSee).map(Player::getName).toList();
        }
        return Collections.emptyList();
    }
}
