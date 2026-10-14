package work.alsace.buildercore.listeners;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CushionListener implements Listener {
    private static final double STEP = 1.0 / 16.0;
    private static final NamespacedKey ADJUSTED = new NamespacedKey("buildercore", "adjusted_cushion");
    private final Map<UUID, Integer> lastAdjustmentTick = new HashMap<>();

    public void register(JavaPlugin plugin) {
        try {
            EntityType.valueOf("CUSHION");
        } catch (IllegalArgumentException olderServer) {
            return;
        }
        // 26.3 的事件通过运行时注册，保留项目现有 Paper 编译版本。
        try {
            Class<? extends Event> type = Class.forName("io.papermc.paper.event.entity.EntityBreakEvent")
                    .asSubclass(Event.class);
            var getCause = type.getMethod("getCause");
            Class<?> byEntityType = Class.forName("io.papermc.paper.event.entity.EntityBreakByEntityEvent");
            var getRemover = byEntityType.getMethod("getRemover");
            plugin.getServer().getPluginManager().registerEvent(type, this, EventPriority.HIGHEST,
                    (listener, event) -> {
                        try {
                            onBreak(((EntityEvent) event).getEntity(), getCause.invoke(event).toString(),
                                    byEntityType.isInstance(event) ? (Entity) getRemover.invoke(event) : null,
                                    (Cancellable) event);
                        } catch (ReflectiveOperationException exception) {
                            throw new EventException(exception);
                        }
                    }, plugin, true);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("坐垫调节需要支持 EntityBreakEvent 的 Paper 26.3 服务端", exception);
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void onBreak(Entity entity, String cause, Entity remover, Cancellable event) {
        if (event.isCancelled()) return;
        // 坐垫在通用攻击事件之前就会拆除，必须在拆除事件中拦截左键。
        if (cause.equals("ENTITY") && remover instanceof Player player && isAdjusting(player, entity)) {
            event.setCancelled(true);
            adjust(player, entity, -STEP);
            return;
        }
        if (entity.getType().name().equals("CUSHION")
                && entity.getPersistentDataContainer().has(ADJUSTED, PersistentDataType.BYTE)
                && (cause.equals("PHYSICS") || cause.equals("OBSTRUCTION"))) {
            event.setCancelled(true);
        }
    }

    private boolean isAdjusting(Player player, Entity target) {
        // 使用实体名称，避免旧版 Paper 加载时依赖尚不存在的 Cushion 接口。
        return target.getType().name().equals("CUSHION") && player.isSneaking()
                && player.getGameMode() != GameMode.SPECTATOR
                && player.getInventory().getItemInMainHand().getType() == Material.BREEZE_ROD;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!isAdjusting(event.getPlayer(), event.getRightClicked())) return;
        event.setCancelled(true);
        if (event.getHand() == EquipmentSlot.HAND) {
            adjust(event.getPlayer(), event.getRightClicked(), STEP);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractAt(PlayerInteractAtEntityEvent event) {
        onInteract(event);
    }

    private void adjust(Player player, Entity cushion, double delta) {
        if (!player.hasPermission("buildercore.protect.interact")
                || !player.hasPermission("buildercore.protect.break")
                || !player.hasPermission("buildercore.protect.place") || !cushion.isValid()) return;
        int tick = player.getTicksLived();
        if (lastAdjustmentTick.getOrDefault(player.getUniqueId(), -1) == tick) return;
        Location destination = cushion.getLocation().add(0, delta, 0);
        if (destination.getY() < cushion.getWorld().getMinHeight()
                || destination.getY() + cushion.getHeight() > cushion.getWorld().getMaxHeight()) return;
        // 客户端可能同一 tick 发送 interact_at 和 interact，合并为一次调节。
        if (cushion.teleport(destination)) {
            cushion.getPersistentDataContainer().set(ADJUSTED, PersistentDataType.BYTE, (byte) 1);
            lastAdjustmentTick.put(player.getUniqueId(), tick);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastAdjustmentTick.remove(event.getPlayer().getUniqueId());
    }
}
