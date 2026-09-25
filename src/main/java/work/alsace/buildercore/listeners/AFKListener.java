package work.alsace.buildercore.listeners;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import work.alsace.buildercore.BuilderCore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AFKListener implements Listener {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final BuilderCore plugin;
    private final Map<UUID, Long> lastActivity = new HashMap<>();

    public AFKListener(BuilderCore plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        startAFKCheckTask();
    }

    private void startAFKCheckTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkAFKPlayers, 20 * 60, 20 * 60);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        lastActivity.put(player.getUniqueId(), System.currentTimeMillis());

        cancelAFK(player);
    }

    @EventHandler
    public void beforePlayerJoin(AsyncPlayerPreLoginEvent loggingEvent) {
        UUID uuid = loggingEvent.getUniqueId();
        lastActivity.put(uuid, System.currentTimeMillis());
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        event.getPlayer().removeMetadata("afk", plugin);
    }

    private void checkAFKPlayers() {
        final long AFK_TIMEOUT_SECONDS = plugin.getConfig().getLong("afk.threshold", 300);
        long currentTime = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            long lastActiveTime = lastActivity.getOrDefault(playerId, currentTime);

            if (currentTime - lastActiveTime > AFK_TIMEOUT_SECONDS * 1000) {
                if (!player.hasPermission("buildercore.afk.bypass")) {
                    handleAFK(player);
                }
            } else {
                if (player.hasMetadata("afk")) {
//                    player.sendMessage(MM.deserialize("<gray>你已取消暂离状态"));
                    player.removeMetadata("afk", plugin);
//                    Bukkit.broadcastMessage(MM.deserialize("<gray>玩家 <white>" + player.getName() + " <gray>回来了"));
                }
            }
        }
    }

    private void handleAFK(Player player) {
        if (!player.hasMetadata("afk")) {
//            player.sendMessage(MM.deserialize("<gray>你已进入暂离状态"));
            player.setMetadata("afk", new FixedMetadataValue(plugin, true));
//            Bukkit.broadcastMessage(MM.deserialize("<gray>玩家 <white>" + player.getName() + " <gray>暂时离开了"));
        }
    }

    private void cancelAFK(Player player) {
        if (player.hasMetadata("afk")) {
//            player.sendMessage(MM.deserialize("<gray>你已取消暂离状态"));
            player.removeMetadata("afk", plugin);
//            Bukkit.broadcastMessage(MM.deserialize("<gray>玩家 <white>" + player.getName() + " <gray>回来了"));
        }
    }
}
