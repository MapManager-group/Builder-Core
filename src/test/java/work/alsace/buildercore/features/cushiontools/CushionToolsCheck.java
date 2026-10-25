package work.alsace.buildercore.features.cushiontools;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.*;
import java.util.function.BiFunction;

/** 无服务器断言；26.3 参数额外检查真实 Cushion 类型的三轴移动。 */
public final class CushionToolsCheck {
    private static final class TestPlugin extends org.bukkit.plugin.java.JavaPlugin { }
    private static <T> T proxy(Class<T> type, BiFunction<String, Object[], Object> calls) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (p, method, args) -> calls.apply(method.getName(), args)));
    }

    public static void main(String[] args) throws Exception {
        UUID worldId = UUID.randomUUID();
        boolean[] loaded = {true};
        WorldBorder border = proxy(WorldBorder.class, (name, values) -> true);
        World world = proxy(World.class, (name, values) -> switch (name) {
            case "getUID" -> worldId;
            case "equals" -> ((World) values[0]).getUID().equals(worldId);
            case "getMinHeight" -> -64;
            case "getMaxHeight" -> 320;
            case "isChunkLoaded" -> loaded[0];
            case "getWorldBorder" -> border;
            default -> throw new AssertionError(name);
        });
        Location first = new Location(world, 0, 64, 0), second = new Location(world, 31, 72, 31);
        assert CushionSession.area(first, second) == 1024;
        assert CushionSession.area(second, first) == 1024;
        assert CushionSession.area(first, new Location(world, 32, 64, 31)) > CushionSession.LIMIT;
        assert CushionSession.withinSelection(new Location(world, .5, 65, .5), first, second);
        assert !CushionSession.withinSelection(new Location(world, 32, 65, .5), first, second);
        assert !CushionSession.withinSelection(new Location(world, .5, 74, .5), first, second);
        assert CushionSession.groundLevels(64, -64, 320).subList(0, 5).equals(List.of(64, 63, 65, 62, 66));
        assert CushionSession.groundLevels(-64, -64, 320).stream().noneMatch(y -> y < -64);
        assert CushionSession.groundLevels(319, -64, 320).stream().noneMatch(y -> y >= 320);
        List<UUID> ids = new ArrayList<>(List.of(UUID.randomUUID()));
        var plan = new CushionSession.Plan(worldId, List.of(), ids, DyeColor.RED, 90);
        ids.clear();
        assert plan.size() == 1;
        assert CushionAccess.loaded(first);
        loaded[0] = false;
        assert !CushionAccess.loaded(first);
        loaded[0] = true;
        assert !CushionAccess.loaded(new Location(world, 0, -65, 0));
        assert !CushionAccess.loaded(new Location(world, 0, 320, 0));

        UUID playerId = UUID.randomUUID();
        ItemStack[][] contents = {new ItemStack[41]};
        int[] slot = {4}, saves = {0};
        boolean[] failSave = {false};
        String[] denied = {""};
        GameMode[] mode = {GameMode.CREATIVE};
        PlayerInventory inventory = proxy(PlayerInventory.class, (name, values) -> switch (name) {
            case "getContents" -> contents[0];
            case "setContents" -> { contents[0] = (ItemStack[]) values[0]; yield null; }
            case "getHeldItemSlot" -> slot[0];
            case "setHeldItemSlot" -> { slot[0] = (int) values[0]; yield null; }
            default -> throw new AssertionError(name);
        });
        Player player = proxy(Player.class, (name, values) -> switch (name) {
            case "getUniqueId" -> playerId;
            case "getInventory" -> inventory;
            case "closeInventory", "setItemOnCursor" -> null;
            case "getGameMode" -> mode[0];
            case "hasPermission" -> !values[0].equals(denied[0]);
            case "getWorld" -> world;
            case "saveData" -> { if (failSave[0]) throw new IllegalStateException("simulated save failure"); saves[0]++; yield null; }
            default -> throw new AssertionError(name);
        });
        for (String permission : List.of("buildercore.commands.cushiontools", "buildercore.protect.interact", "buildercore.protect.place", "buildercore.protect.break")) {
            denied[0] = permission;
            assert !CushionAccess.permitted(player);
        }
        denied[0] = "";
        assert CushionAccess.permitted(player);
        mode[0] = GameMode.SPECTATOR;
        assert !CushionAccess.permitted(player);
        mode[0] = GameMode.CREATIVE;

        Path directory = Files.createTempDirectory("cushion-backup-check-");
        Path file = directory.resolve(playerId + ".dat");
        InventoryBackup backup = new InventoryBackup(directory);
        try {
            backup.save(player);
            assert backup.exists(playerId);
            byte[] original = Files.readAllBytes(file);
            try { backup.save(player); throw new AssertionError("overwrote backup"); } catch (IOException expected) { }
            assert Arrays.equals(original, Files.readAllBytes(file));
            slot[0] = 0;
            failSave[0] = true;
            try { backup.restore(player); throw new AssertionError("ignored player save failure"); } catch (IllegalStateException expected) { }
            assert backup.exists(playerId);
            failSave[0] = false;
            backup.restore(player);
            assert slot[0] == 4 && saves[0] == 1 && !backup.exists(playerId);
            backup.save(player);
            Files.write(file, new byte[]{0, 0, 0});
            ItemStack[] before = contents[0];
            try { backup.restore(player); throw new AssertionError("accepted truncated backup"); } catch (IOException expected) { }
            assert contents[0] == before && backup.exists(playerId);
        } finally {
            Files.deleteIfExists(file);
            Files.delete(directory);
        }
        if (args.length > 0) movement(player, world, denied);
        System.out.println("CushionTools selection, permissions and inventory recovery checks passed");
    }

    private static void movement(Player player, World world, String[] denied) throws Exception {
        Location[] position = {new Location(world, 10.5, 65, 20.5)};
        Map<NamespacedKey, Object> data = new HashMap<>();
        PersistentDataContainer pdc = proxy(PersistentDataContainer.class, (name, values) -> {
            if (name.equals("set")) data.put((NamespacedKey) values[0], values[2]);
            return null;
        });
        boolean[] passenger = {false};
        Entity cushion = proxy(Entity.class, (name, values) -> switch (name) {
            case "getType" -> EntityType.valueOf("CUSHION");
            case "getUniqueId" -> new UUID(0, 1);
            case "getWorld" -> world;
            case "getLocation" -> position[0].clone();
            case "isValid" -> true;
            case "isEmpty" -> !passenger[0];
            case "getPersistentDataContainer" -> pdc;
            case "teleport" -> { position[0] = ((Location) values[0]).clone(); yield true; }
            default -> throw new AssertionError(name);
        });
        CushionAccess access = new CushionAccess(null);
        for (org.bukkit.util.Vector axis : List.of(new org.bukkit.util.Vector(1, 0, 0), new org.bukkit.util.Vector(0, 1, 0), new org.bukkit.util.Vector(0, 0, 1))) {
            Location before = position[0].clone();
            assert access.move(player, cushion, before.clone().add(axis.clone().multiply(CushionSession.STEP)));
            assert position[0].distance(before) == CushionSession.STEP;
            assert access.move(player, cushion, before);
        }
        assert data.get(new NamespacedKey("buildercore", "adjusted_cushion")).equals((byte) 1);
        denied[0] = "buildercore.protect.place";
        assert !access.move(player, cushion, position[0].clone().add(0, 1, 0));
        assert access.rollback(cushion, new Location(world, 10.5, 65, 20.5));
        denied[0] = "";
        passenger[0] = true;
        assert !access.move(player, cushion, position[0].clone().add(0, 1, 0));
        assert !access.rollback(cushion, position[0]);
        passenger[0] = false;
        events(player, world, cushion, position, access);
        System.out.println("CushionTools 26.3 three-axis movement and interaction checks passed");
    }

    private static void field(Class<?> owner, Object object, String name, Object value) throws Exception {
        var field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }

    private static void events(Player original, World world, Entity cushion, Location[] position, CushionAccess access) throws Exception {
        int[] tick = {1}, selected = {1};
        PersistentDataContainer tags = proxy(PersistentDataContainer.class, (name, values) -> selected[0]);
        var meta = proxy(org.bukkit.inventory.meta.ItemMeta.class, (name, values) -> tags);
        ItemStack held = new ItemStack() {
            @Override public boolean hasItemMeta() { return true; }
            @Override public org.bukkit.inventory.meta.ItemMeta getItemMeta() { return meta; }
        };
        PlayerInventory inventory = proxy(PlayerInventory.class, (name, values) -> held);
        Player player = proxy(Player.class, (name, values) -> switch (name) {
            case "getUniqueId" -> original.getUniqueId();
            case "getInventory" -> inventory;
            case "getTicksLived" -> tick[0];
            case "getWorld" -> world;
            case "getGameMode" -> original.getGameMode();
            case "hasPermission" -> original.hasPermission((String) values[0]);
            default -> throw new AssertionError(name);
        });
        // 与现有 SelectedFixesCheck 相同，只注入离线事件检查实际需要的字段。
        var unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        var unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        CushionTools tools = (CushionTools) unsafe.allocateInstance(CushionTools.class);
        CushionSession state = new CushionSession();
        field(CushionTools.class, tools, "sessions", new HashMap<>(Map.of(player.getUniqueId(), state)));
        field(CushionTools.class, tools, "access", access);
        double y = position[0].getY();
        var right = new org.bukkit.event.player.PlayerInteractAtEntityEvent(player, cushion, new org.bukkit.util.Vector(), org.bukkit.inventory.EquipmentSlot.HAND);
        tools.entityInteractAt(right);
        assert right.isCancelled() && position[0].getY() == y + CushionSession.STEP;
        tools.entityInteract(new org.bukkit.event.player.PlayerInteractEntityEvent(player, cushion, org.bukkit.inventory.EquipmentSlot.HAND));
        assert position[0].getY() == y + CushionSession.STEP;
        tick[0]++;
        tools.entityInteract(new org.bukkit.event.player.PlayerInteractEntityEvent(player, cushion, org.bukkit.inventory.EquipmentSlot.OFF_HAND));
        assert position[0].getY() == y + CushionSession.STEP;
        var blocked = new org.bukkit.event.player.PlayerInteractEntityEvent(player, cushion, org.bukkit.inventory.EquipmentSlot.HAND);
        blocked.setCancelled(true);
        tools.entityInteract(blocked);
        assert position[0].getY() == y + CushionSession.STEP;
        var attack = new io.papermc.paper.event.player.PrePlayerAttackEntityEvent(player, cushion, true);
        tools.attack(attack);
        assert attack.isCancelled() && position[0].getY() == y;
        tick[0]++;
        state.recovering = true;
        tools.use(player, cushion, true);
        assert position[0].getY() == y;
        state.recovering = false;

        org.bukkit.plugin.EventExecutor[] executor = {null};
        var manager = proxy(org.bukkit.plugin.PluginManager.class, (name, values) -> {
            if (name.equals("registerEvent")) executor[0] = (org.bukkit.plugin.EventExecutor) values[3];
            else throw new AssertionError(name);
            return null;
        });
        Server server = proxy(Server.class, (name, values) -> {
            if (name.equals("getPluginManager")) return manager;
            throw new AssertionError(name);
        });
        org.bukkit.plugin.java.JavaPlugin plugin = (org.bukkit.plugin.java.JavaPlugin) unsafe.allocateInstance(TestPlugin.class);
        field(org.bukkit.plugin.java.JavaPlugin.class, plugin, "server", server);
        CushionAccess nativeAccess = new CushionAccess(plugin);
        assert nativeAccess.register(tools);
        Class<?> breakType = Class.forName("io.papermc.paper.event.entity.EntityBreakByEntityEvent");
        Class<?> causeType = Class.forName("io.papermc.paper.event.entity.EntityBreakEvent$RemoveCause");
        Object entityCause = causeType.getField("ENTITY").get(null);
        var constructor = breakType.getConstructor(Entity.class, Entity.class, Class.forName("org.bukkit.damage.DamageSource"), causeType);
        var selectedConstructor = CushionAccess.class.getDeclaredField("breakConstructor");
        selectedConstructor.setAccessible(true);
        assert ((java.lang.reflect.Constructor<?>) selectedConstructor.get(nativeAccess)).getParameterCount() == 4;
        var firstLeft = (org.bukkit.event.Event) constructor.newInstance(cushion, player, null, entityCause);
        executor[0].execute(tools, firstLeft);
        assert ((org.bukkit.event.Cancellable) firstLeft).isCancelled();
        assert position[0].getY() == y - CushionSession.STEP;
        var duplicate = (org.bukkit.event.Event) constructor.newInstance(cushion, player, null, entityCause);
        executor[0].execute(tools, duplicate);
        assert position[0].getY() == y - CushionSession.STEP;
        tick[0]++;
        var protectedBreak = (org.bukkit.event.Event) constructor.newInstance(cushion, player, null, entityCause);
        ((org.bukkit.event.Cancellable) protectedBreak).setCancelled(true);
        executor[0].execute(tools, protectedBreak);
        assert position[0].getY() == y - CushionSession.STEP;
    }

}
