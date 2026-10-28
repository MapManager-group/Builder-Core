package work.alsace.buildercore.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Slab.Type;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;

import java.util.HashSet;
import java.util.Set;

public class BlockListener implements Listener {
    public static Set<Player> slabs = new HashSet<>();
    private static final Set<String> ADMIN_BLOCKS = Set.of(
            "COMMAND_BLOCK", "CHAIN_COMMAND_BLOCK", "REPEATING_COMMAND_BLOCK",
            "JIGSAW", "STRUCTURE_BLOCK", "STRUCTURE_VOID", "BARRIER",
            "TEST_BLOCK", "TEST_INSTANCE_BLOCK", "LIGHT");

    public BlockListener() {
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAdminBlockInteract(PlayerInteractEvent event) {
        Block clicked = event.getClickedBlock();
        if (clicked == null || event.getHand() != EquipmentSlot.HAND
                || event.useItemInHand() == org.bukkit.event.Event.Result.DENY
                || event.getPlayer().getGameMode() == org.bukkit.GameMode.SPECTATOR) return;
        Player player = event.getPlayer();
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && ADMIN_BLOCKS.contains(clicked.getType().name())) {
            event.setCancelled(true);
            if (!player.hasPermission("buildercore.protect.break")) return;
            BlockBreakEvent breakEvent = new BlockBreakEvent(clicked, player);
            Bukkit.getPluginManager().callEvent(breakEvent);
            if (!breakEvent.isCancelled()) clicked.setType(Material.AIR);
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (ADMIN_BLOCKS.contains(clicked.getType().name())) event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        if (!ADMIN_BLOCKS.contains(item.getType().name()) || !player.hasPermission("buildercore.protect.place")) return;
        Block target = clicked.getRelative(event.getBlockFace());
        if (!target.isReplaceable()) return;
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        BlockState replaced = target.getState();
        target.setType(item.getType(), false);
        BlockPlaceEvent placeEvent = new BlockPlaceEvent(target, replaced, clicked, item, player, true, EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(placeEvent);
        if (placeEvent.isCancelled() || !placeEvent.canBuild()) {
            replaced.update(true, false);
        } else if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }
    }

    @EventHandler
    public void onBlockPhysics(BlockFadeEvent event) {
        preventCoralDecay(event);
    }

    static void preventCoralDecay(BlockFadeEvent event) {
        if (event.getBlock().getType().name().contains("CORAL")) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteractLight(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.LIGHT || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getHand() != EquipmentSlot.HAND || event.useItemInHand() == org.bukkit.event.Event.Result.DENY
                || event.getPlayer().getGameMode() == org.bukkit.GameMode.SPECTATOR
                || !event.getPlayer().hasPermission("buildercore.protect.interact")
                || !event.getPlayer().hasPermission("buildercore.protect.place")) return;
        Levelled data = (Levelled) block.getBlockData();
        data.setLevel((data.getLevel() + 1) % (data.getMaximumLevel() + 1));
        block.setBlockData(data, true);
        event.setCancelled(true);
    }

    @EventHandler(
            priority = EventPriority.MONITOR, ignoreCancelled = true
    )
    public void onBlockBreak(BlockBreakEvent e) {
        if (!e.isCancelled() && slabs.contains(e.getPlayer()) && e.getPlayer().hasPermission("buildercore.commands.slab")) {
            Material type = e.getPlayer().getInventory().getItemInMainHand().getType();
            if (type.toString().toLowerCase(java.util.Locale.ROOT).contains("slab")) {
                if (e.getBlock().getType().toString().toLowerCase(java.util.Locale.ROOT).contains("slab")) {
                    Slab blockData;
                    if (this.isTop(e.getPlayer(), e.getBlock())) {
                        blockData = (Slab) e.getBlock().getBlockData();
                        if (blockData.getType().equals(Type.DOUBLE)) {
                            blockData.setType(Type.BOTTOM);
                            e.getBlock().setBlockData(blockData, true);
                            e.setCancelled(true);
                        }
                    } else {
                        blockData = (Slab) e.getBlock().getBlockData();
                        if (blockData.getType().equals(Type.DOUBLE)) {
                            blockData.setType(Type.TOP);
                            e.getBlock().setBlockData(blockData, true);
                            e.setCancelled(true);
                        }
                    }
                }
            }

        }
    }

    private boolean isTop(Player player, Block block) {
        Location eye = player.getEyeLocation();
        var hit = block.rayTrace(eye, eye.getDirection(), 64, org.bukkit.FluidCollisionMode.NEVER);
        return hit != null && hit.getHitPosition().getY() - block.getY() > 0.5;
    }
}
