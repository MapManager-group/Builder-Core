package work.alsace.buildercore.listeners;

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
        if (!e.getPlayer().hasPermission("buildercore.protect.break")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Component.text("你没有权限破坏方块").color(NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (!e.getPlayer().hasPermission("buildercore.protect.place")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Component.text("你没有权限放置方块").color(NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) { return; }
        if (e.getClickedBlock() != null && e.getClickedBlock().getState() instanceof Lectern) { return; }
        if (!e.getPlayer().hasPermission("buildercore.protect.interact")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Component.text("你没有权限与方块交互").color(NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onLectern(PlayerInteractEvent e){
        if (!e.getPlayer().hasPermission("buildercore.protect.lectern") && e.getAction() == Action.RIGHT_CLICK_BLOCK) {
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
        if (player != null && !player.hasPermission("buildercore.protect.ignite")) {
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

        if (!e.getPlayer().hasPermission("buildercore.protect.sign")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Component.text("你没有权限修改告示牌内容").color(NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onHangingPlace(HangingPlaceEvent e) {
        if (e.getPlayer() != null && !e.getPlayer().hasPermission("buildercore.protect.item_frame")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Component.text("你没有权限放置展示框").color(NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onHangingBreakByEntity(HangingBreakByEntityEvent e) {
        Player player = attackingPlayer(e.getRemover());

        if (player != null) {
            if (isSword(player)) {
                e.setCancelled(true);
                return;
            }

            if (!player.hasPermission("buildercore.protect.item_frame")) {
                e.setCancelled(true);
                player.sendMessage(Component.text("你没有权限破坏展示框").color(NamedTextColor.RED));
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        Player player = attackingPlayer(e.getDamager());
        if (player == null) return;
        if (isSword(player)) {
            e.setCancelled(true);
            return;
        }
        String permission;
        String message;
        if (e.getEntity() instanceof org.bukkit.entity.ItemFrame) {
            permission = "buildercore.protect.item_frame";
            message = "你没有权限破坏展示框";
        } else if (e.getEntity() instanceof org.bukkit.entity.ArmorStand) {
            permission = "buildercore.protect.armor_stand";
            message = "你没有权限破坏盔甲架";
        } else return;
        if (!player.hasPermission(permission)) {
            e.setCancelled(true);
            player.sendMessage(Component.text(message, NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        if (!e.getPlayer().hasPermission("buildercore.protect.item_frame") && e.getRightClicked().getType().toString().toUpperCase(java.util.Locale.ROOT).contains("ITEM_FRAME")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Component.text("你没有权限放置展示框").color(NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onPlayerInteractP(PlayerInteractEvent e) {
        if (e.getAction() == Action.PHYSICAL && !e.getPlayer().hasPermission("buildercore.protect.farmland")) {
            if (e.getClickedBlock() == null) { return; }

            Material blockType = e.getClickedBlock().getType();
            if (blockType == Material.FARMLAND) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerBodyInteract(PlayerInteractEvent e) {
        if (e.getAction() == Action.PHYSICAL && !e.getPlayer().hasPermission("buildercore.protect.interact")) {
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

        if (!e.getPlayer().hasPermission("buildercore.protect.armor_stand")) {
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

        if (!e.getPlayer().hasPermission("buildercore.protect.armor_stand") && e.getPlayer().getInventory().getItemInMainHand().getType() == Material.ARMOR_STAND) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Component.text("你没有权限使用盔甲架").color(NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteractSword(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (isSword(player)) {
            event.setCancelled(true);
        }
    }

    /** 兼容旧调用入口，实体伤害统一由 onEntityDamageByEntity 处理。 */
    public void onArmorEntityDamageByEntity(EntityDamageByEntityEvent event) {
        onEntityDamageByEntity(event);
    }

    private Player attackingPlayer(org.bukkit.entity.Entity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        return null;
    }

    private boolean isSword(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        return item.getType().name().contains("_SWORD");
    }
}
