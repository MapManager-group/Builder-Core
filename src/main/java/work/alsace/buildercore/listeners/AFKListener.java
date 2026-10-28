package work.alsace.buildercore.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import work.alsace.buildercore.BuilderCore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AFKListener implements Listener {
    private final BuilderCore plugin;
    private final Map<UUID, Long> lastActivity = new HashMap<>();

    public AFKListener(BuilderCore plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player player : Bukkit.getOnlinePlayers()) lastActivity.put(player.getUniqueId(), System.currentTimeMillis());

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
    public void onPlayerJoin(PlayerJoinEvent loggingEvent) {
        UUID uuid = loggingEvent.getPlayer().getUniqueId();
        lastActivity.put(uuid, System.currentTimeMillis());
    }
    /** 兼容旧入口；活动记录统一在主线程更新。 */
    public void beforePlayerJoin(org.bukkit.event.player.AsyncPlayerPreLoginEvent event) {
        UUID id = event.getUniqueId();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (Bukkit.getPlayer(id) != null) lastActivity.put(id, System.currentTimeMillis());
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        lastActivity.remove(event.getPlayer().getUniqueId());
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
            }
        }
    }

    private void handleAFK(Player player) {
        if (!player.hasMetadata("afk")) {
            player.setMetadata("afk", new FixedMetadataValue(plugin, true));
        }
    }

    private void cancelAFK(Player player) {
        if (player.hasMetadata("afk")) {
            player.removeMetadata("afk", plugin);
        }
    }
}
