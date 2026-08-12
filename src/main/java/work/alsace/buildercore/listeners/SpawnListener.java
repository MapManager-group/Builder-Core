package work.alsace.buildercore.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SpawnListener implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null) return;

        String materialName = item.getType().name();

        if (!materialName.endsWith("SPAWN_EGG")) return;

        Action action = event.getAction();

        if (action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
        } else {
            return;
        }

        if (materialName.equals("ENDER_DRAGON_SPAWN_EGG")) {
            event.getPlayer().sendMessage(ChatColor.RED + "你无法使用末影龙刷怪蛋！");
            return;
        }

        if (!event.getPlayer().isSneaking()) {
             event.getPlayer().sendMessage(ChatColor.GRAY + "请潜行(Shift)并点击方块来放置实体。");
            return;
        }

        if (!event.getPlayer().hasPermission("buildercore.protect.place")) return;

        spawnCustomEntity(event, action == Action.LEFT_CLICK_BLOCK);
    }

    /**
     * 统一处理实体生成的逻辑
     *
     * @param event  交互事件
     * @param isBaby 是否生成幼年实体
     */
    private void spawnCustomEntity(PlayerInteractEvent event, boolean isBaby) {
        String materialName = event.getItem().getType().name();
        EntityType entityType;

        try {
            if (materialName.contains("MOOSHROOM")) {
                // 兼容不同版本的哞菇枚举名
                entityType = EntityType.valueOf("MUSHROOM_COW");
            } else {
                entityType = EntityType.valueOf(materialName.replace("_SPAWN_EGG", ""));
            }
        } catch (IllegalArgumentException e) {
            // 防止未能正确匹配到实体类型导致报错
            return;
        }

        // 使用 clone() 防止修改原方块的坐标，并让坐标居中(+0.5)
        Location spawnLoc = event.getClickedBlock().getLocation().clone().add(0.5, 1.0, 0.5);
        Location playerLoc = event.getPlayer().getLocation();

        // 提前计算朝向角度 (Yaw)
        double deltaX = playerLoc.getX() - spawnLoc.getX();
        double deltaZ = playerLoc.getZ() - spawnLoc.getZ();
        double angle = Math.atan2(deltaZ, deltaX);
        float newYaw = (float) (Math.toDegrees(angle) - 90);

        // 将角度直接赋予生成的坐标点，避免生成后再 teleport
        spawnLoc.setYaw(newYaw);
        spawnLoc.setPitch(0);

        // 生成实体
        Entity entity = event.getPlayer().getWorld().spawnEntity(spawnLoc, entityType);

        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.setAI(false); // 禁用 AI

            // 处理幼崽逻辑
            if (isBaby && livingEntity instanceof Ageable) {
                ((Ageable) livingEntity).setBaby();
            }
        }
    }
}