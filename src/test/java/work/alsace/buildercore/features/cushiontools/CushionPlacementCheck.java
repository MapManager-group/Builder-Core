package work.alsace.buildercore.features.cushiontools;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Colorable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.BiFunction;

/** 验证右键工具从交互、地面检测到生成，并尊重完整取消和实体放置保护。 */
public final class CushionPlacementCheck {
    private static final class TestPlugin extends JavaPlugin { }
    private static <T> T proxy(Class<T> type, BiFunction<String, Object[], Object> calls) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (p, method, args) -> calls.apply(method.getName(), args)));
    }
    private static void field(Class<?> type, Object target, String name, Object value) throws Exception {
        var field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
    public static void main(String[] args) throws Exception {
        int[] tick = {1}, spawns = {0}, removed = {0}, messages = {0};
        boolean[] denyPlace = {false}, denyPermission = {false}, occupied = {false};
        DyeColor[] appliedColor = {null};
        Location[] spawnedAt = {null};
        UUID id = UUID.randomUUID(), worldId = UUID.randomUUID();
        World[] worlds = {null};
        PersistentDataContainer entityTags = proxy(PersistentDataContainer.class, (name, values) -> null);
        Entity cushion = (Entity) Proxy.newProxyInstance(Entity.class.getClassLoader(), new Class<?>[]{Entity.class, Colorable.class},
                (p, method, values) -> switch (method.getName()) {
                    case "setColor" -> { appliedColor[0] = (DyeColor) values[0]; yield null; }
                    case "getPersistentDataContainer" -> entityTags;
                    case "isValid" -> true;
                    case "remove" -> { removed[0]++; yield null; }
                    default -> throw new AssertionError(method.getName());
                });
        Block floor = proxy(Block.class, (name, values) -> switch (name) {
            case "getWorld" -> worlds[0];
            case "getX", "getZ" -> 0;
            case "getY" -> 64;
            case "isLiquid", "isPassable" -> false;
            case "getType" -> Material.STONE;
            case "rayTrace" -> new RayTraceResult(new Vector(.5, 65, .5), BlockFace.UP);
            default -> throw new AssertionError(name);
        });
        Block air = proxy(Block.class, (name, values) -> switch (name) {
            case "isLiquid" -> false;
            case "isPassable" -> true;
            default -> throw new AssertionError(name);
        });
        WorldBorder border = proxy(WorldBorder.class, (name, values) -> true);
        World world = proxy(World.class, (name, values) -> switch (name) {
            case "getUID" -> worldId;
            case "equals" -> ((World) values[0]).getUID().equals(worldId);
            case "getMinHeight" -> -64;
            case "getMaxHeight" -> 320;
            case "getWorldBorder" -> border;
            case "isChunkLoaded" -> true;
            case "getBlockAt" -> values.length == 1 || (int) values[1] == 64 ? floor : air;
            case "getNearbyEntities" -> List.of();
            case "rayTraceBlocks" -> occupied[0] ? new RayTraceResult(new Vector(.5, 65.1, .5), BlockFace.DOWN) : null;
            case "spawnEntity" -> { spawns[0]++; spawnedAt[0] = ((Location) values[0]).clone(); yield cushion; }
            default -> throw new AssertionError(name);
        });
        worlds[0] = world;
        PersistentDataContainer toolTags = proxy(PersistentDataContainer.class, (name, values) -> 4);
        ItemMeta meta = proxy(ItemMeta.class, (name, values) -> toolTags);
        ItemStack tool = new ItemStack() {
            @Override public boolean hasItemMeta() { return true; }
            @Override public ItemMeta getItemMeta() { return meta; }
        };
        PlayerInventory inventory = proxy(PlayerInventory.class, (name, values) -> tool);
        Player player = proxy(Player.class, (name, values) -> switch (name) {
            case "getUniqueId" -> id;
            case "getTicksLived" -> tick[0];
            case "getWorld" -> world;
            case "getInventory" -> inventory;
            case "getGameMode" -> GameMode.CREATIVE;
            case "hasPermission" -> !denyPermission[0] || !values[0].equals("buildercore.protect.place");
            case "sendMessage", "sendActionBar" -> { messages[0]++; yield null; }
            default -> throw new AssertionError(name);
        });
        PluginManager manager = proxy(PluginManager.class, (name, values) -> {
            if (!name.equals("callEvent")) throw new AssertionError(name);
            ((EntityPlaceEvent) values[0]).setCancelled(denyPlace[0]);
            return null;
        });
        Server server = proxy(Server.class, (name, values) -> manager);
        var unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        var unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        JavaPlugin plugin = (JavaPlugin) unsafe.allocateInstance(TestPlugin.class);
        field(JavaPlugin.class, plugin, "server", server);
        CushionAccess access = new CushionAccess(plugin);
        field(CushionAccess.class, access, "type", args.length == 0 ? EntityType.ARMOR_STAND : EntityType.valueOf("CUSHION"));
        CushionTools tools = (CushionTools) unsafe.allocateInstance(CushionTools.class);
        CushionSession session = new CushionSession();
        session.color = DyeColor.RED;
        session.yaw = 90;
        field(CushionTools.class, tools, "sessions", new HashMap<>(Map.of(id, session)));
        field(CushionTools.class, tools, "access", access);
        java.util.function.Supplier<PlayerInteractEvent> click = () -> new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, tool, floor, BlockFace.UP, EquipmentSlot.HAND);
        tools.interact(click.get());
        assert spawns[0] == 1 && appliedColor[0] == DyeColor.RED;
        assert spawnedAt[0].getY() == 65 && spawnedAt[0].getYaw() == 90;
        tools.interact(click.get());
        assert spawns[0] == 1 : "same-tick duplicate placement";
        tick[0]++;
        PlayerInteractEvent blockOnlyDenied = click.get();
        blockOnlyDenied.setUseInteractedBlock(Event.Result.DENY);
        tools.interact(blockOnlyDenied);
        assert spawns[0] == 2 : "block-only denial must not disable tool placement";
        assert blockOnlyDenied.useInteractedBlock() == Event.Result.DENY && blockOnlyDenied.useItemInHand() == Event.Result.DENY;
        tick[0]++;
        PlayerInteractEvent cancelled = click.get();
        cancelled.setCancelled(true);
        tools.interact(cancelled);
        assert spawns[0] == 2 && messages[0] > 0 : "full cancellation must remain protected and visible";
        tick[0]++;
        PlayerInteractEvent itemDenied = click.get();
        itemDenied.setUseItemInHand(Event.Result.DENY);
        tools.interact(itemDenied);
        assert spawns[0] == 2 : "item-only denial must prevent tool use";
        tick[0]++;
        denyPermission[0] = true;
        int before = messages[0];
        tools.interact(click.get());
        assert spawns[0] == 2 && messages[0] > before : "permission denial must remain protected and visible";
        denyPermission[0] = false;
        tick[0]++;
        tools.interact(new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, tool, floor, BlockFace.UP, EquipmentSlot.OFF_HAND));
        assert spawns[0] == 2;
        denyPlace[0] = true;
        tools.interact(click.get());
        assert spawns[0] == 3 && removed[0] == 1 : "EntityPlaceEvent denial must roll back spawn";
        tick[0]++;
        occupied[0] = true;
        tools.interact(click.get());
        assert spawns[0] == 3 : "blocked headroom must prevent spawn";
        System.out.println("Cushion quick-placement interaction, ground, style, deduplication and protection checks passed");
    }
}
