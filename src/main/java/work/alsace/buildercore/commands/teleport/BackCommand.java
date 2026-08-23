package work.alsace.buildercore.commands.teleport;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import work.alsace.buildercore.BuilderCore;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BackCommand implements TabExecutor, Listener {

    private final BuilderCore plugin;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Map<UUID, Deque<Location>> locationHistory = new ConcurrentHashMap<>();

    public BackCommand(BuilderCore plugin) {
        this.plugin = plugin;
        // 注册玩家离线监听器，用于清除内存缓存
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 1) {
            // 提供 1 到 maxHistory 的步数补全提示
            int max = Math.max(1, plugin.backHistory);
            return java.util.stream.IntStream.rangeClosed(1, max)
                    .mapToObj(String::valueOf)
                    .filter(s -> s.startsWith(args[0]))
                    .toList();
        }
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sendMsg(sender, "<red>该命令只能由玩家使用");
            return true;
        }

        if (!player.hasPermission("buildercore.commands.back")) {
            sendMsg(player, "<red>你没有使用该命令的权限");
            return true;
        }

        Deque<Location> history = locationHistory.get(player.getUniqueId());
        if (history == null || history.isEmpty()) {
            sendMsg(player, "<red>没有位置记录");
            return true;
        }

        int maxHistory = Math.max(1, plugin.backHistory);
        int backSteps = 1;

        if (args.length > 0) {
            try {
                backSteps = Integer.parseInt(args[0]);
                if (backSteps < 1 || backSteps > maxHistory) {
                    sendMsg(player, "<red>无效的参数，步数只能在 1 到 " + maxHistory + " 之间");
                    return true;
                }
            } catch (NumberFormatException e) {
                sendMsg(player, "<red>无效的参数，请输入数字");
                return true;
            }
        }

        if (history.size() < backSteps) {
            sendMsg(player, "<red>没有足够的位置记录（当前仅有 " + history.size() + " 条）");
            return true;
        }

        // 出栈指定步数
        Location targetLocation = null;
        for (int i = 0; i < backSteps; i++) {
            targetLocation = history.pollLast(); // 弹出最新入栈的位置
        }

        if (targetLocation != null) {
            final int finalSteps = backSteps;
            player.teleportAsync(targetLocation).thenAccept(success -> {
                if (success) {
                    sendMsg(player, "<gray>已返回到之前的第 <white>" + finalSteps + "</white> 个位置");
                } else {
                    sendMsg(player, "<red>传送失败，请稍后再试");
                }
            });
        }

        return true;
    }

    /**
     * 记录玩家当前位置到历史栈中
     */
    public void addToHistory(Player player) {
        UUID uuid = player.getUniqueId();
        Location currentLocation = player.getLocation().clone();

        Deque<Location> history = locationHistory.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        history.addLast(currentLocation);

        // 维持栈大小不超出限制
        int maxHistory = Math.max(1, plugin.backHistory);
        while (history.size() > maxHistory) {
            history.pollFirst();
        }
    }

    /**
     * 玩家离线时清理内存缓存，防止内存泄漏
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        locationHistory.remove(event.getPlayer().getUniqueId());
    }

    private void sendMsg(CommandSender sender, String miniMessageText) {
        sender.sendMessage(MM.deserialize(miniMessageText));
    }
}