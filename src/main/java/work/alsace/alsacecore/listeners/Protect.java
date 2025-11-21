package work.alsace.alsacecore.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Lectern;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class Protect implements Listener {

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (!e.getPlayer().hasPermission("alsace.protect.break")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Component.text("你没有权限破坏方块").color(NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (!e.getPlayer().hasPermission("alsace.protect.place")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Component.text("你没有权限放置方块").color(NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) { return; }
        if (e.getClickedBlock() != null && e.getClickedBlock().getState() instanceof Lectern) { return; }
        if (!e.getPlayer().hasPermission("alsace.protect.interact")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Component.text("你没有权限与方块交互").color(NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onLectern(PlayerInteractEvent e){
        if (!e.getPlayer().hasPermission("alsace.protect.lectern") && e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (Objects.requireNonNull(e.getClickedBlock()).getState() instanceof Lectern) {
                Lectern lectern = (Lectern) e.getClickedBlock().getState();
                if (lectern.getInventory().getItem(0) == null) { return; }
                e.setCancelled(true);
                e.getPlayer().openBook(Objects.requireNonNull(lectern.getInventory().getItem(0)));
            }
        }
    }

    @EventHandler
    public void onIgnite(BlockIgniteEvent e) {
        Player player = e.getPlayer();
        if (player != null && !player.hasPermission("alsace.protect.ignite")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Component.text("你没有权限点燃方块").color(NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent e) {
        if (isSword(e.getPlayer())) {
            e.setCancelled(true);
            return;
        }

        if (!e.getPlayer().hasPermission("alsace.protect.sign")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Component.text("你没有权限修改告示牌内容").color(NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onHangingPlace(HangingPlaceEvent e) {
        if (e.getPlayer() != null && !e.getPlayer().hasPermission("alsace.protect.item_frame")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Component.text("你没有权限放置展示框").color(NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onHangingBreakByEntity(HangingBreakByEntityEvent e) {
        Player player = null;
        if (e.getRemover() instanceof Player) {
            player = (Player)e.getRemover();
        } else if (e.getRemover() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                player = (Player)projectile.getShooter();
            }
        }

        if (player != null) {
            if (isSword(player)) {
                e.setCancelled(true);
                return;
            }

            if (!player.hasPermission("alsace.protect.item_frame")) {
                e.setCancelled(true);
                player.sendMessage(Component.text("你没有权限破坏展示框").color(NamedTextColor.RED));
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        Player player = null;
        if (e.getDamager() instanceof Player) {
            player = (Player) e.getDamager();
        } else if (e.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                player = (Player) projectile.getShooter();
            }
        }
        if (player != null) {
            if (isSword(player)) {
                e.setCancelled(true);
                return;
            }

            if (e.getEntity().getType().toString().toUpperCase().contains("ITEM_FRAME") && !player.hasPermission("alsace.protect.item_frame")) {
                e.setCancelled(true);
                player.sendMessage(Component.text("你没有权限破坏展示框").color(NamedTextColor.RED));
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        if (!e.getPlayer().hasPermission("alsace.protect.item_frame") && e.getRightClicked().getType().toString().toUpperCase().contains("ITEM_FRAME")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Component.text("你没有权限放置展示框").color(NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onDragonEgg(BlockFromToEvent e) {
        if (e.getBlock().getType() == Material.DRAGON_EGG) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractP(PlayerInteractEvent e) {
        if (e.getAction() == Action.PHYSICAL && !e.getPlayer().hasPermission("alsace.protect.farmland")) {
            if (e.getClickedBlock() == null) { return; }

            Material blockType = e.getClickedBlock().getType();
            if (blockType == Material.FARMLAND) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerBodyInteract(PlayerInteractEvent e) {
        if (e.getAction() == Action.PHYSICAL && !e.getPlayer().hasPermission("alsace.protect.interact")) {
            if (e.getClickedBlock() == null) { return; }
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
        if (isSword(e.getPlayer())) {
            e.setCancelled(true);
            return;
        }

        if (!e.getPlayer().hasPermission("alsace.protect.armor_stand")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Component.text("你没有权限修改盔甲架").color(NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onArmorPlayerInteract(PlayerInteractEvent e) {
        if (isSword(e.getPlayer())) {
            e.setCancelled(true);
            return;
        }

        if (!e.getPlayer().hasPermission("alsace.protect.armor_stand") && e.getPlayer().getInventory().getItemInMainHand().getType() == Material.ARMOR_STAND) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Component.text("你没有权限使用盔甲架").color(NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onArmorEntityDamageByEntity(EntityDamageByEntityEvent e) {
        Player player = null;
        if (e.getDamager() instanceof Player) {
            player = (Player) e.getDamager();
        } else if (e.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                player = (Player) projectile.getShooter();
            }
        }
        if (player != null) {
            if (isSword(player)) {
                e.setCancelled(true);
                return;
            }

            if (e.getEntity().getType().toString().toUpperCase().contains("ARMOR_STAND") && !player.hasPermission("alsace.protect.armor_stand")) {
                e.setCancelled(true);
                player.sendMessage(Component.text("你没有权限破坏盔甲架").color(NamedTextColor.RED));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteractSword(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (isSword(player)) {
            event.setCancelled(true);
        }
    }

    private boolean isSword(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        return item.getType().name().contains("_SWORD");
    }
}
