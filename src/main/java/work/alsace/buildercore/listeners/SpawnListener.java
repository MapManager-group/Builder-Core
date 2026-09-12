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

    // 记录玩家二次确认移除的数据
    private final Map<UUID, RemovalConfirm> confirmCache = new ConcurrentHashMap<>();
    private static final long CONFIRM_TIMEOUT_MS = 3000L; // 3秒超时

    private record RemovalConfirm(UUID entityUuid, long timestamp) {
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        // 判断是否为刷怪蛋
        if (!(item.getItemMeta() instanceof SpawnEggMeta eggMeta)) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        event.setCancelled(true);

        // 阻止末影龙刷怪蛋
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
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        if (!player.hasPermission("buildercore.protect.place")) return;

        // 盔甲架和展示实体不参与二次点击清除
        if (event.getEntity() instanceof ArmorStand || event.getEntity() instanceof Display) return;

        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;

        // 取消原有的攻击伤害逻辑
        event.setCancelled(true);

        UUID playerUuid = player.getUniqueId();
        UUID entityUuid = livingEntity.getUniqueId();
        long currentTime = System.currentTimeMillis();

        RemovalConfirm record = confirmCache.get(playerUuid);

        // 判断是否在 3 秒内连续点击同一个实体
        if (record != null && record.entityUuid().equals(entityUuid) && (currentTime - record.timestamp()) <= CONFIRM_TIMEOUT_MS) {
            // 确认移除
            confirmCache.remove(playerUuid);
            livingEntity.remove();
            player.sendMessage(Component.text("已成功清除该实体！", NamedTextColor.GREEN));
        } else {
            // 首次点击，写入确认缓存并发送提醒
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
            // 兜底机制：从物品材质获取默认 EntityType
            entityType = getEntityTypeFromMaterial(event.getItem().getType());
            if (entityType == null) return;
        }

        Player player = event.getPlayer();
        Location spawnLoc = event.getClickedBlock().getLocation().add(0.5, 1.0, 0.5);
        Location playerLoc = player.getLocation();

        // 算面向玩家的朝向角度 (Yaw)
        double deltaX = playerLoc.getX() - spawnLoc.getX();
        double deltaZ = playerLoc.getZ() - spawnLoc.getZ();
        float newYaw = (float) (Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90);

        spawnLoc.setYaw(newYaw);
        spawnLoc.setPitch(0);

        player.getWorld().spawn(spawnLoc, entityType.getEntityClass(), entity -> {
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.setAI(ai);
                livingEntity.setGlowing(false);
                livingEntity.setInvulnerable(true);

                // 处理幼崽逻辑
                if (isBaby && livingEntity instanceof Ageable ageable) {
                    ageable.setBaby();
                }
            }
        });
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