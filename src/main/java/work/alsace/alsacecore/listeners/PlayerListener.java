package work.alsace.alsacecore.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import work.alsace.alsacecore.AlsaceCore;
import work.alsace.alsacecore.commands.builderTools.AdvanceFlyCommand;
import work.alsace.alsacecore.commands.teleport.BackCommand;
import work.alsace.alsacecore.service.HomeDataLoader;
import work.alsace.alsacecore.utils.NoClipUtil;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PlayerListener implements Listener {
    private final AlsaceCore plugin;

    public PlayerListener(AlsaceCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        BlockListener.slabs.add(event.getPlayer());
        plugin.homeProfiles.put(event.getPlayer().getUniqueId(), new HomeDataLoader(event.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        Player player = event.getPlayer();
        this.plugin.hasIgnored.remove(player.getName());
        plugin.homeProfiles.remove(player.getUniqueId());
        if (NoClipUtil.noclip.contains(player) || AdvanceFlyCommand.enabledPlayers.contains(player) || BlockListener.slabs.contains(player)) {
            NoClipUtil.noclip.remove(player);
            AdvanceFlyCommand.enabledPlayers.remove(player);
            BlockListener.slabs.remove(player);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (plugin.hasAgree.getOrDefault(player.getName(), false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause().equals(TeleportCause.SPECTATE) && !event.getPlayer().hasPermission("alsace.event.spectate")) {
            event.setCancelled(true);
        }

        // 检查/back命令是否已注册
        if (plugin.getCommand("back") != null) {
            BackCommand backCommand = (BackCommand) Objects.requireNonNull(plugin.getCommand("back")).getExecutor();
            backCommand.addToHistory(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        plugin.homeProfiles.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void playerChangeSign(SignChangeEvent event) {
        boolean cnt = false;
    }

    @EventHandler
    public void playerAnvil(PrepareAnvilEvent event) {
        ItemStack item = event.getResult();
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String name = meta.getDisplayName();
                name = ChatColor.translateAlternateColorCodes('&', name);
                meta.setDisplayName(this.translateHexColorCodes(name));
                item.setItemMeta(meta);
                event.setResult(item);
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    private String translateHexColorCodes(String message) {
        Pattern hexPattern = Pattern.compile("#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        StringBuilder buffer = new StringBuilder(message.length() + 32);

        while (matcher.find()) {
            String group = matcher.group(1);
            char var10002 = group.charAt(0);
            matcher.appendReplacement(buffer, "§x§" + var10002 + "§" + group.charAt(1) + "§" + group.charAt(2) + "§" + group.charAt(3) + "§" + group.charAt(4) + "§" + group.charAt(5));
        }

        return matcher.appendTail(buffer).toString();
    }
}
