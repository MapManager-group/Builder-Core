package work.alsace.buildercore.features.cushiontools;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.material.Colorable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/** 将 26.3 的实体、拆除事件隔离在此边界，继续按项目现有 Paper API 编译。 */
final class CushionAccess {
    private static final NamespacedKey ADJUSTED = new NamespacedKey("buildercore", "adjusted_cushion");
    private final JavaPlugin plugin;
    private final Set<UUID> removing = new HashSet<>();
    private EntityType type;
    private Constructor<?> breakConstructor;
    private Object entityBreakCause;

    CushionAccess(JavaPlugin plugin) { this.plugin = plugin; }

    boolean register(CushionTools tools) {
        try {
            type = EntityType.valueOf("CUSHION");
            Class<? extends Event> eventType = Class.forName("io.papermc.paper.event.entity.EntityBreakEvent").asSubclass(Event.class);
            var cause = eventType.getMethod("getCause");
            Class<?> byEntity = Class.forName("io.papermc.paper.event.entity.EntityBreakByEntityEvent");
            var remover = byEntity.getMethod("getRemover");
            Class<?> causeType = Class.forName("io.papermc.paper.event.entity.EntityBreakEvent$RemoveCause");
            entityBreakCause = causeType.getField("ENTITY").get(null);
            breakConstructor = byEntity.getConstructor(Entity.class, Entity.class, DamageSource.class, causeType);
            plugin.getServer().getPluginManager().registerEvent(eventType, tools, EventPriority.HIGHEST,
                    (listener, event) -> {
                        try {
                            Entity cushion = ((EntityEvent) event).getEntity();
                            if (!isCushion(cushion) || removing.contains(cushion.getUniqueId())) return;
                            Cancellable cancellable = (Cancellable) event;
                            String reason = cause.invoke(event).toString();
                            if (reason.equals("ENTITY") && byEntity.isInstance(event)
                                    && remover.invoke(event) instanceof Player player && tools.active(player)) {
                                boolean allowed = !cancellable.isCancelled();
                                cancellable.setCancelled(true);
                                if (allowed) tools.use(player, cushion, false);
                            } else if (cushion.getPersistentDataContainer().has(ADJUSTED, PersistentDataType.BYTE)
                                    && (reason.equals("PHYSICS") || reason.equals("OBSTRUCTION"))) {
                                cancellable.setCancelled(true);
                            }
                        } catch (ReflectiveOperationException exception) {
                            throw new EventException(exception);
                        }
                    }, plugin);
            return true;
        } catch (IllegalArgumentException | ReflectiveOperationException exception) {
            plugin.getLogger().log(Level.WARNING, "坐垫工具 API 检测失败：" + exception, exception);
            return false;
        }
    }

    static boolean isCushion(Entity entity) { return entity != null && entity.getType().name().equals("CUSHION"); }

    static boolean permitted(Player player) {
        return player.getGameMode() != GameMode.SPECTATOR
                && player.hasPermission("buildercore.commands.cushiontools")
                && player.hasPermission("buildercore.protect.interact")
                && player.hasPermission("buildercore.protect.place")
                && player.hasPermission("buildercore.protect.break");
    }

    static boolean loaded(Location location) {
        World world = location.getWorld();
        return world != null && world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)
                && location.getY() >= world.getMinHeight() && location.getY() + 0.25 <= world.getMaxHeight()
                && world.getWorldBorder().isInside(location);
    }

    static void mark(Entity cushion) {
        cushion.getPersistentDataContainer().set(ADJUSTED, PersistentDataType.BYTE, (byte) 1);
    }

    boolean move(Player player, Entity cushion, Location destination) {
        if (!permitted(player) || !isCushion(cushion) || !cushion.isValid() || !cushion.isEmpty()
                || !player.getWorld().equals(cushion.getWorld()) || !cushion.getWorld().equals(destination.getWorld())
                || !loaded(destination)) return false;
        if (!cushion.teleport(destination)) return false;
        mark(cushion);
        return true;
    }

    boolean rollback(Entity cushion, Location origin) {
        if (!isCushion(cushion) || !cushion.isValid() || !cushion.isEmpty() || !loaded(origin)
                || !cushion.getWorld().equals(origin.getWorld())) return false;
        return cushion.teleport(origin);
    }

    boolean place(Player player, Location location, DyeColor color, float yaw) {
        if (!permitted(player) || !player.getWorld().equals(location.getWorld()) || !loaded(location)) return false;
        World world = location.getWorld();
        if (world.getNearbyEntities(location, 0.45, 0.2, 0.45, CushionAccess::isCushion).size() > 0) return false;
        location = location.clone();
        location.setYaw(yaw);
        location.setPitch(0);
        Entity cushion = world.spawnEntity(location, type);
        try {
            ((Colorable) cushion).setColor(color);
            mark(cushion);
            Block support = world.getBlockAt(location.clone().subtract(0, 0.01, 0));
            EntityPlaceEvent event = new EntityPlaceEvent(cushion, player, support, BlockFace.UP, EquipmentSlot.HAND);
            plugin.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled() || !cushion.isValid()) {
                cushion.remove();
                return false;
            }
            return true;
        } catch (RuntimeException exception) {
            cushion.remove();
            throw exception;
        }
    }

    boolean remove(Player player, Entity cushion) {
        if (!permitted(player) || !isCushion(cushion) || !cushion.isValid() || !cushion.isEmpty()
                || !player.getWorld().equals(cushion.getWorld()) || !loaded(cushion.getLocation())) return false;
        removing.add(cushion.getUniqueId());
        try {
            Event event = (Event) breakConstructor.newInstance(cushion, player,
                    DamageSource.builder(DamageType.PLAYER_ATTACK).withCausingEntity(player).withDirectEntity(player).build(), entityBreakCause);
            plugin.getServer().getPluginManager().callEvent(event);
            if (((Cancellable) event).isCancelled()) return false;
            cushion.remove();
            return true;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("无法触发坐垫拆除事件", exception);
        } finally {
            removing.remove(cushion.getUniqueId());
        }
    }
}
