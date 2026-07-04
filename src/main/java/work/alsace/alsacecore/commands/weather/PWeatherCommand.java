package work.alsace.alsacecore.commands.weather;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PWeatherCommand implements TabExecutor {
    private final String error = ChatColor.GRAY + "正确指令:\n§f/pweather <天气> §7- 设置你自己客户端的天气\n§f/pweather <天气> <玩家> §7- 设置指定玩家的客户端天气";
    private static final List<String> weathers = Arrays.asList("clear", "rain");

    @Override
    public boolean onCommand(@NotNull CommandSender cmdSender, @NotNull Command cmd, @NotNull String s, String[] args) {
        if (!(cmdSender instanceof Player)) {
            cmdSender.sendMessage(ChatColor.RED + "该命令只能由玩家执行");
            return false;
        }

        if (!cmdSender.hasPermission("buildercore.commands.pweather")) {
            cmdSender.sendMessage(ChatColor.RED + "你没有使用该命令的权限");
            return false;
        }

        Player player = (Player) cmdSender;

        if (args.length == 0 || args.length > 2) {
            player.sendMessage(error);
            return false;
        }

        String weather = args[0].toLowerCase();
        if (!weathers.contains(weather)) {
            player.sendMessage(ChatColor.RED + "请输入有效的天气类型: clear, rain");
            return false;
        }

        if (args.length == 1) {
            return setWeather(player, weather);
        }

        Player targetPlayer = player.getServer().getPlayer(args[1]);
        if (targetPlayer == null) {
            player.sendMessage(ChatColor.RED + "玩家 " + args[1] + " 不存在或不在线");
            return true;
        }

        return setWeather(targetPlayer, weather);
    }

    private boolean setWeather(Player player, String weather) {
        switch (weather) {
            case "clear" -> {
                player.resetPlayerWeather();
                player.sendMessage("已将"+player.getName() +"的世界天气设置为 §f晴天");
                return true;
            }
            case "rain" -> {
                player.setPlayerWeather(org.bukkit.WeatherType.DOWNFALL);
                player.sendMessage("已将"+player.getName() +"的世界天气设置为 §f雨天");
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
        return Collections.emptyList();
    }
}
