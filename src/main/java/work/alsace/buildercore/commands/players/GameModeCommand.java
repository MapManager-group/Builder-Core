package work.alsace.buildercore.commands.players;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class GameModeCommand implements TabExecutor {

    private static final List<String> MODE_SUGGESTIONS = List.of("creative", "survival", "adventure", "spectator");
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String USAGE = """
            <gray>正确指令:
            <white>/gamemode <模式></white> - 设置你的游戏模式
            <white>/gamemode <模式> [玩家]</white> - 设置指定玩家的游戏模式</gray>""";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length < 1 || args.length > 2) {
            sendMsg(sender, USAGE);
            return true;
        }

        // 解析游戏模式
        GameMode gameMode = parseGameMode(args[0]);
        if (gameMode == null) {
            sendMsg(sender, "<red>无效的游戏模式！\n" + USAGE);
            return true;
        }

        // 修改指定玩家模式 (/gamemode <模式> <玩家>)
        if (args.length == 2) {
            if (!sender.hasPermission("buildercore.commands.gamemode.other")) {
                sendMsg(sender, "<red>你没有修改其他玩家游戏模式的权限");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null || !target.isOnline()) {
                sendMsg(sender, "<red>指定的玩家不在线或不存在");
                return true;
            }

            target.setGameMode(gameMode);
            sendMsg(target, "<gray>你的游戏模式已被设置为 <white>" + gameMode.name().toLowerCase() + "</white>");
            sendMsg(sender, "<gray>已将玩家 <white>" + target.getName() + "</white> 的游戏模式设置为 <white>" + gameMode.name().toLowerCase() + "</white>");
            return true;
        }

        // 修改自身模式 (/gamemode <模式>)
        if (!(sender instanceof Player player)) {
            sendMsg(sender, "<red>控制台必须指定目标玩家：/gamemode <模式> <玩家>");
            return true;
        }

        if (!player.hasPermission("buildercore.commands.gamemode")) {
            sendMsg(player, "<red>你没有使用该命令的权限");
            return true;
        }

        player.setGameMode(gameMode);
        sendMsg(player, "<gray>已将你的游戏模式设置为 <white>" + gameMode.name().toLowerCase() + "</white>");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1 && sender.hasPermission("buildercore.commands.gamemode")) {
            String prefix = args[0].toLowerCase();
            return MODE_SUGGESTIONS.stream()
                    .filter(m -> m.startsWith(prefix))
                    .toList();
        }

        if (args.length == 2 && sender.hasPermission("buildercore.commands.gamemode.other")) {
            String prefix = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .toList();
        }

        return Collections.emptyList();
    }

    private GameMode parseGameMode(String input) {
        return switch (input.toLowerCase()) {
            case "creative", "c", "1" -> GameMode.CREATIVE;
            case "survival", "s", "0" -> GameMode.SURVIVAL;
            case "adventure", "a", "2" -> GameMode.ADVENTURE;
            case "spectator", "sp", "3" -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    private void sendMsg(CommandSender sender, String miniMessageText) {
        sender.sendMessage(MM.deserialize(miniMessageText));
    }
}