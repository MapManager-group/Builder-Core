package work.alsace.alsacecore.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import work.alsace.alsacecore.AlsaceCore;

public class VanishListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasMetadata("vanish")) {
            if (!AlsaceCore.instance.hasIgnored.getOrDefault(player.getName(), false))
                player.performCommand("tpignore");
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other.getUniqueId().equals(player.getUniqueId()))
                    continue;
                other.hidePlayer(AlsaceCore.instance, player);
            }
        } else {
            for (Player other : Bukkit.getOnlinePlayers())
                if (other.hasMetadata("vanish"))
                    player.hidePlayer(AlsaceCore.instance, other);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

    }
}
