package work.alsace.buildercore.features;

import org.bukkit.GameMode;
import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent;
import org.bukkit.entity.EntityType;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

/** Building controls adapted to BuilderCore's existing permission model. */
public final class BuildingSettings implements Listener {
    private final JavaPlugin plugin;
    private final BuilderTools features;

    public BuildingSettings(JavaPlugin plugin, BuilderTools features) {
        this.plugin = plugin;
        this.features = features;
    }

    private boolean setting(String name) {
        return plugin.getConfig().getBoolean("building-settings." + name, false);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent event) {
        if (!setting("disable-redstone") && !setting("disable-gravity-physics") && !setting("disable-physics")) return;
        Material type = event.getBlock().getType();
        if (Tag.STAIRS.isTagged(type) || Tag.FENCES.isTagged(type) || Tag.FENCE_GATES.isTagged(type)
                || Tag.WALLS.isTagged(type) || Tag.DOORS.isTagged(type) || type == Material.CHEST
                || type == Material.TRAPPED_CHEST || type == Material.ENDER_CHEST) return;
        if (type == Material.REDSTONE_WIRE || type == Material.REDSTONE_LAMP || type == Material.REPEATER
                || type == Material.COMPARATOR || type == Material.PISTON || type == Material.STICKY_PISTON
                || type == Material.OBSERVER || type == Material.LEVER || Tag.BUTTONS.isTagged(type)) {
            if (setting("disable-redstone")) event.setCancelled(true);
        } else if (event.getChangedType().hasGravity()) {
            if (setting("disable-gravity-physics")) event.setCancelled(true);
        } else if (setting("disable-physics")) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (setting("disable-entity-explode")) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (setting("disable-block-explode")) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplosionDamage(EntityDamageEvent event) {
        if ((event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION && setting("disable-entity-explode"))
                || (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION && setting("disable-block-explode"))) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (setting("disable-leaves-decay")) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDragonEgg(BlockFromToEvent event) {
        if (setting("disable-dragon-egg-teleport") && event.getBlock().getType() == Material.DRAGON_EGG) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpectatorTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE
                && !event.getPlayer().hasPermission("buildercore.spectate.teleport")) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpectate(PlayerStartSpectatingEntityEvent event) {
        String permission = event.getNewSpectatorTarget().getType() == EntityType.PLAYER
                ? "buildercore.spectate.player" : "buildercore.spectate.entity";
        if (!event.getPlayer().hasPermission(permission)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL && setting("disable-farmland-trample")
                && event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.FARMLAND) {
            event.setCancelled(true);
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND
                || event.getClickedBlock() == null) return;
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.CREATIVE || !player.hasPermission("buildercore.protect.interact")
                || !player.hasPermission("buildercore.protect.break") || !player.hasPermission("buildercore.protect.place")) return;
        Block block = event.getClickedBlock();
        if (features.isEnabled(player, "iron-door") && player.hasPermission("buildercore.ability.irondoor")
                && (block.getType() == Material.IRON_DOOR || block.getType() == Material.IRON_TRAPDOOR)
                && (!player.isSneaking() || player.getInventory().getItemInMainHand().getType() == Material.AIR)) {
            Openable door = (Openable) block.getBlockData();
            door.setOpen(!door.isOpen());
            block.setBlockData(door);
            event.setCancelled(true);
        } else if (features.isEnabled(player, "terracotta") && player.hasPermission("buildercore.ability.terracotta")
                && block.getType().name().endsWith("_GLAZED_TERRACOTTA") && player.isSneaking()
                && player.getInventory().getItemInMainHand().getType() == Material.AIR) {
            Directional data = (Directional) block.getBlockData();
            data.setFacing(switch (data.getFacing()) {
                case NORTH -> BlockFace.EAST;
                case EAST -> BlockFace.SOUTH;
                case SOUTH -> BlockFace.WEST;
                case WEST -> BlockFace.NORTH;
                default -> data.getFacing();
            });
            block.setBlockData(data);
            event.setCancelled(true);
        }
    }
}
