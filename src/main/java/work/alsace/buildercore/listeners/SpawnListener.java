package work.alsace.buildercore.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SpawnEggMeta;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnListener implements Listener {

    private final Map<UUID, RemovalConfirm> confirmCache = new ConcurrentHashMap<>();
    private static final long CONFIRM_TIMEOUT_MS = 3000L; // 3秒超时

    private record RemovalConfirm(UUID entityUuid, long timestamp) {
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.useItemInHand() == org.bukkit.event.Event.Result.DENY
                || event.getPlayer().getGameMode() == org.bukkit.GameMode.SPECTATOR) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        if (!(item.getItemMeta() instanceof SpawnEggMeta eggMeta)) return;
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND
                && event.getPlayer().getInventory().getItemInMainHand().getItemMeta() instanceof SpawnEggMeta) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        event.setCancelled(true);

        if (item.getType() == Material.ENDER_DRAGON_SPAWN_EGG) {
            player.sendMessage(Component.text("你无法使用末影龙刷怪蛋！", NamedTextColor.RED));
            return;
        }

        if (!player.hasPermission("buildercore.protect.place")) return;

        spawnCustomEntity(event, eggMeta, action == Action.LEFT_CLICK_BLOCK, !player.isSneaking());
    }

    /**
     * 监听左键攻击实体事件，需连续点击两次确认移除
     */
    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        if (!player.hasPermission("buildercore.protect.place") || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) return;

        if (event.getEntity() instanceof Player || event.getEntity() instanceof ArmorStand || event.getEntity() instanceof Display) return;

        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;

        event.setCancelled(true);

        UUID playerUuid = player.getUniqueId();
        UUID entityUuid = livingEntity.getUniqueId();
        long currentTime = System.currentTimeMillis();

        RemovalConfirm record = confirmCache.get(playerUuid);

        if (record != null && record.entityUuid().equals(entityUuid) && (currentTime - record.timestamp()) <= CONFIRM_TIMEOUT_MS) {
            confirmCache.remove(playerUuid);
            livingEntity.remove();
            player.sendMessage(Component.text("已成功清除该实体！", NamedTextColor.GREEN));
        } else {
            confirmCache.put(playerUuid, new RemovalConfirm(entityUuid, currentTime));
            player.sendMessage(Component.text("再次左键以确认移除该实体（3秒内有效）。", NamedTextColor.YELLOW));
        }
    }

    /**
     * 统一处理实体生成的逻辑
     *
     * @param event   交互事件
     * @param eggMeta 刷怪蛋 Meta
     * @param isBaby  是否生成幼年实体
     * @param ai      是否开启 AI
     */
    private void spawnCustomEntity(PlayerInteractEvent event, SpawnEggMeta eggMeta, boolean isBaby, boolean ai) {
        if (event.getClickedBlock() == null) return;

        EntityType entityType = eggMeta.getCustomSpawnedType();
        if (entityType == null) {
            entityType = getEntityTypeFromMaterial(event.getItem().getType());
            if (entityType == null) return;
        }

        if (entityType == EntityType.ENDER_DRAGON || !entityType.isSpawnable() || entityType.getEntityClass() == null) return;
        Player player = event.getPlayer();
        Location spawnLoc = event.getClickedBlock().getLocation().add(0.5, 1.0, 0.5);
        Location playerLoc = player.getLocation();

        double deltaX = playerLoc.getX() - spawnLoc.getX();
        double deltaZ = playerLoc.getZ() - spawnLoc.getZ();
        float newYaw = (float) (Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90);

        spawnLoc.setYaw(newYaw);
        spawnLoc.setPitch(0);

        Entity spawned = player.getWorld().spawn(spawnLoc, entityType.getEntityClass(), entity -> {
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.setAI(ai);
                livingEntity.setGlowing(false);
                livingEntity.setInvulnerable(true);

                if (isBaby && livingEntity instanceof Ageable ageable) {
                    ageable.setBaby();
                }
            }
        });
        var placed = new org.bukkit.event.entity.EntityPlaceEvent(spawned, player, event.getClickedBlock(), event.getBlockFace(), event.getHand());
        org.bukkit.Bukkit.getPluginManager().callEvent(placed);
        if (placed.isCancelled()) spawned.remove();
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        confirmCache.remove(event.getPlayer().getUniqueId());
    }

    /**
     * 从刷怪蛋材质名兜底解析 EntityType
     */
    private EntityType getEntityTypeFromMaterial(Material material) {
        try {
            String name = material.name();
            if (name.contains("MOOSHROOM")) {
                return EntityType.valueOf("MUSHROOM_COW");
            }
            return EntityType.valueOf(name.replace("_SPAWN_EGG", ""));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
