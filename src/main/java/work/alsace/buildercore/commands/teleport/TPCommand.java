package work.alsace.buildercore.commands.teleport;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import work.alsace.buildercore.BuilderCore;
import work.alsace.buildercore.service.TeleportAccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class TPCommand implements TabExecutor {
    private final BuilderCore plugin;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public TPCommand(BuilderCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length != 1 || !(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        String prefix = args[0].toLowerCase();
        return getAccessPlayers(player).stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix))
                .toList(); // Java 16+ 原生 toList()
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        switch (args.length) {
            case 1 -> handleSelfToPlayer(sender, args[0]);
            case 2 -> handlePlayerToPlayer(sender, args[0], args[1]);
            case 3 -> handleSelfToLocation(sender, args[0], args[1], args[2]);
            case 4 -> handlePlayerToLocation(sender, args[0], args[1], args[2], args[3]);
            default -> sendMsg(sender, "<red>参数错误，正确用法：/tp [玩家|x] [目标玩家|y] [z] [玩家]</red>");
        }
        return true;
    }

    // 1. /tp <targetPlayer>
    private void handleSelfToPlayer(CommandSender sender, String targetName) {
        if (!(sender instanceof Player commander)) {
            sendMsg(sender, "<red>该指令仅限玩家执行");
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sendMsg(commander, "<red>玩家 " + targetName + " 不在线");
            return;
        }

        if (!commander.hasPermission("buildercore.commands.tp.player")) {
            sendMsg(commander, "<red>你没有使用该命令的权限");
            return;
        }

        // 屏蔽传送检查
        if (isIgnored(target.getName())) {
            sendMsg(commander, "<red>玩家 " + target.getName() + " 已屏蔽强制传送");
            return;
        }

        teleportAfterDelay(commander, target);
    }

    // 2. /tp <player1> <player2>
    private void handlePlayerToPlayer(CommandSender sender, String p1Name, String p2Name) {
        if (!sender.hasPermission("buildercore.commands.tp.player") || !sender.hasPermission("buildercore.commands.tp.other")) {
            sendMsg(sender, "<red>你没有使用该命令的权限");
            return;
        }

        Player commander = Bukkit.getPlayer(p1Name);
        Player target = Bukkit.getPlayer(p2Name);

        if (commander == null) {
            sendMsg(sender, "<red>玩家 " + p1Name + " 不在线");
            return;
        }
        if (target == null) {
            sendMsg(sender, "<red>玩家 " + p2Name + " 不在线");
            return;
        }

        if (teleportAfterDelay(commander, target)) {
            sendMsg(sender, "<gray>已将 <white>" + commander.getName() + "</white> 传送至 <white>" + target.getName() + "</white>");
        }
    }

    // 3. /tp <x> <y> <z>
    private void handleSelfToLocation(CommandSender sender, String xStr, String yStr, String zStr) {
        if (!(sender instanceof Player commander)) {
            sendMsg(sender, "<red>该指令仅限玩家执行");
            return;
        }

        if (!commander.hasPermission("buildercore.commands.tp.location")) {
            sendMsg(commander, "<red>你没有使用该命令的权限");
            return;
        }

        parseLocation(commander, commander, xStr, yStr, zStr).ifPresentOrElse(
                loc -> {
                    if (!TeleportAccess.canTeleport(plugin, commander, loc.getWorld(), true)) {
                        sendMsg(commander, "<red>你没有目标世界的建筑权限，无法直接传送");
                        return;
                    }
                    commander.teleportAsync(loc);
                    sendMsg(commander, String.format("<gray>已将 %s 传送至 (%.2f, %.2f, %.2f)", commander.getName(), loc.getX(), loc.getY(), loc.getZ()));
                },
                () -> sendMsg(commander, "<red>输入坐标有误，请重新输入")
        );
    }

    // 4. /tp <player|@s> <x> <y> <z>
    private void handlePlayerToLocation(CommandSender sender, String targetStr, String xStr, String yStr, String zStr) {
        if (!sender.hasPermission("buildercore.commands.tp.location")) {
            sendMsg(sender, "<red>你没有使用该命令的权限");
            return;
        }

        Player target = "@s".equalsIgnoreCase(targetStr) && sender instanceof Player p ? p : Bukkit.getPlayer(targetStr);

        if (target == null) {
            sendMsg(sender, "<red>目标玩家 " + targetStr + " 不在线");
            return;
        }

        // 跨人传送权限检查
        if (!sender.equals(target) && !sender.hasPermission("buildercore.commands.tp.other")) {
            sendMsg(sender, "<red>你没有传送其他玩家的权限");
            return;
        }

        // 获取基准参考坐标（如果是控制台，相对坐标由 target 决定）
        CommandSender baseSender = (sender instanceof Player p) ? p : target;

        parseLocation(baseSender, target, xStr, yStr, zStr).ifPresentOrElse(
                loc -> {
                    if (!TeleportAccess.canTeleport(plugin, target, loc.getWorld(), true)) {
                        sendMsg(sender, "<red>目标玩家没有目标世界的建筑权限，无法直接传送");
                        return;
                    }
                    target.teleportAsync(loc);
                    sendMsg(sender, String.format("<gray>已将 %s 传送至 (%.2f, %.2f, %.2f)", target.getName(), loc.getX(), loc.getY(), loc.getZ()));
                },
                () -> sendMsg(sender, "<red>输入坐标有误，请重新输入")
        );
    }

    // 解析坐标
    private Optional<Location> parseLocation(CommandSender sender, Player target, String xStr, String yStr, String zStr) {
        try {
            Location origin = (sender instanceof Player p) ? p.getLocation() : target.getLocation();
            double x = parseCoordinate(xStr, origin.getX());
            double y = parseCoordinate(yStr, origin.getY());
            double z = parseCoordinate(zStr, origin.getZ());

            return Optional.of(new Location(target.getWorld(), x, y, z, origin.getYaw(), origin.getPitch()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private double parseCoordinate(String input, double defaultValue) {
        if (input.startsWith("~")) {
            if (input.length() == 1) {
                return defaultValue;
            }
            return defaultValue + Double.parseDouble(input.substring(1)); // 修复：直接截取从 1 到结尾
        }
        return Double.parseDouble(input);
    }

    private boolean isIgnored(String playerName) {
        return Boolean.TRUE.equals(this.plugin.hasIgnored.get(playerName));
    }

    private List<Player> getAccessPlayers(Player sender) {
        List<Player> players = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach((player) -> {
            if (TeleportAccess.canTeleport(plugin, sender, player.getWorld(), true))
                players.add(player);
        });
        return players;
    }

    private boolean teleportAfterDelay(Player fromPlayer, Player toPlayer) {
        if (!TeleportAccess.canTeleport(plugin, fromPlayer, toPlayer.getWorld(), true)) {
            sendMsg(fromPlayer, "<red>你没有目标世界的建筑权限，无法直接传送");
            return false;
        }
        sendMsg(fromPlayer, "<gray>正在传送至 <white>" + toPlayer.getName() + "</white>...");

        if (!fromPlayer.hasMetadata("vanish")) {
            sendMsg(toPlayer, "<white>" + fromPlayer.getName() + "</white> <gray>正在传送到你身边...");
        }

        // 延迟 1 秒后使用异步传送 API
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (fromPlayer.isOnline() && toPlayer.isOnline()) {
                if (!TeleportAccess.canTeleport(plugin, fromPlayer, toPlayer.getWorld(), true)) {
                    sendMsg(fromPlayer, "<red>你没有目标世界的建筑权限，无法直接传送");
                    return;
                }
                fromPlayer.teleportAsync(toPlayer.getLocation()).thenAccept(success -> {
                    if (success) {
                        sendMsg(fromPlayer, "<gray>你已成功传送到 <white>" + toPlayer.getName() + "</white> 身边！");
                    }
                });
            }
        }, 20L);
        return true;
    }

    private void sendMsg(CommandSender sender, String miniMessageText) {
        sender.sendMessage(MM.deserialize(miniMessageText));
    }
}