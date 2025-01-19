package work.alsace.alsacecore.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import work.alsace.alsacecore.AlsaceCore;

import java.io.IOException;
import java.sql.SQLException;

public class InventoryListener implements Listener {

    private final AlsaceCore plugin;
    public InventoryListener(AlsaceCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String inventory = plugin.getLiteDataBaseManager().loadPlayerInventory(uuid);
                if (inventory != null) {
                    ItemStack[] inventoryContents = plugin.getSerializeInventory().deserializeInventory(inventory);
                    player.getInventory().setContents(inventoryContents);
                }
            } catch (IOException | SQLException e) {
                plugin.getLogger().severe("[背包同步] 加载玩家背包数据失败: " + player.getName() + ": " + e.getMessage());
            }
        });

        try {
            Location location = plugin.getLiteDataBaseManager().loadPlayerLocation(uuid);
            if (location != null) {
                player.teleport(location);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[背包同步] 加载玩家位置数据失败: " + player.getName() + ": " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();

        ItemStack[] inventoryContents = player.getInventory().getContents();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String inventory = plugin.getSerializeInventory().serializeInventory(inventoryContents);
                plugin.getLiteDataBaseManager().savePlayerInventory(uuid, inventory);
            } catch (IOException | SQLException e) {
                plugin.getLogger().severe("[背包同步] 保存玩家背包数据失败: " + player.getName() + ": " + e.getMessage());
            }
        });

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getLiteDataBaseManager().savePlayerLocation(uuid, player.getLocation());
            } catch (SQLException e) {
                plugin.getLogger().severe("[背包同步] 保存玩家位置数据失败: " + player.getName() + ": " + e.getMessage());
            }
        });
    }
}
