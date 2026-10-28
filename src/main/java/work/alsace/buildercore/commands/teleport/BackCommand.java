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

    private final Map<UUID, Location> returning = new java.util.HashMap<>();

    public BackCommand(BuilderCore plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 1) {
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
        if (work.alsace.buildercore.commands.CommandHelp.requested(sender, cmd, label, args)) return true;
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

        if (returning.containsKey(player.getUniqueId())) {
            sendMsg(player, "<red>正在返回，请稍候");
            return true;
        }
        var positions = history.descendingIterator();
        java.util.Set<Location> consumed = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        Location targetLocation = null;
        for (int i = 0; i < backSteps; i++) {
            targetLocation = positions.next();
            consumed.add(targetLocation);
        }
        if (targetLocation.getWorld() == null) {
            sendMsg(player, "<red>目标世界未加载，位置记录已保留");
            return true;
        }
        final int steps = backSteps;
        returning.put(player.getUniqueId(), targetLocation);
        player.teleportAsync(targetLocation).whenComplete((success, error) -> {
            Runnable finish = () -> {
                returning.remove(player.getUniqueId());
                if (error == null && Boolean.TRUE.equals(success)) {
                    history.removeIf(consumed::contains);
                    sendMsg(player, "<gray>已返回到之前的第 <white>" + steps + "</white> 个位置");
                } else sendMsg(player, "<red>传送失败，位置记录已保留");
            };
            if (org.bukkit.Bukkit.isPrimaryThread()) finish.run();
            else plugin.getServer().getScheduler().runTask(plugin, finish);
        });

        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(org.bukkit.event.player.PlayerTeleportEvent event) {
        if (!java.util.Objects.equals(returning.get(event.getPlayer().getUniqueId()), event.getTo())) addToHistory(event.getPlayer());
    }

    /**
     * 记录玩家当前位置到历史栈中
     */
    public void addToHistory(Player player) {
        UUID uuid = player.getUniqueId();
        Location currentLocation = player.getLocation().clone();

        Deque<Location> history = locationHistory.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        history.addLast(currentLocation);

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
        returning.remove(event.getPlayer().getUniqueId());
    }

    private void sendMsg(CommandSender sender, String miniMessageText) {
        sender.sendMessage(MM.deserialize(miniMessageText));
    }
}
