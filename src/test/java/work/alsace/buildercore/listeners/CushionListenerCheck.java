package work.alsace.buildercore.listeners;

import org.bukkit.Location;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.util.Vector;

import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/** 无需服务器的升降、权限、重复事件与边界检查；用 java -ea 运行。 */
public final class CushionListenerCheck {
    private static final class HeldItem extends ItemStack {
        Material type = Material.BREEZE_ROD;

        @Override
        public Material getType() { return type; }
    }

    private static <T> T proxy(Class<T> type, BiFunction<String, Object[], Object> calls) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (p, method, args) -> calls.apply(method.getName(), args)));
    }

    public static void main(String[] args) throws Exception {
        UUID id = UUID.randomUUID();
        int[] tick = {1};
        String[] denied = {""};
        boolean[] valid = {true};
        boolean[] teleportSucceeds = {true};
        int[] moves = {0};
        boolean[] sneaking = {true};
        GameMode[] mode = {GameMode.CREATIVE};
        EntityType[] type = {args.length == 0 ? EntityType.ARMOR_STAND : EntityType.valueOf("CUSHION")};
        HeldItem item = new HeldItem();
        PlayerInventory inventory = proxy(PlayerInventory.class, (name, values) -> item);
        Map<NamespacedKey, Object> data = new HashMap<>();
        PersistentDataContainer container = proxy(PersistentDataContainer.class, (name, values) -> switch (name) {
            case "set" -> { data.put((NamespacedKey) values[0], values[2]); yield null; }
            case "has" -> data.containsKey(values[0]);
            default -> throw new AssertionError(name);
        });
        World world = proxy(World.class, (name, values) -> switch (name) {
            case "getMinHeight" -> -64;
            case "getMaxHeight" -> 320;
            default -> throw new AssertionError(name);
        });
        Location[] position = {new Location(world, 10.5, 65, 20.5, 90, 0)};
        Player player = proxy(Player.class, (name, values) -> switch (name) {
            case "hasPermission" -> !values[0].equals(denied[0]);
            case "getUniqueId" -> id;
            case "getTicksLived" -> tick[0];
            case "isSneaking" -> sneaking[0];
            case "getGameMode" -> mode[0];
            case "getInventory" -> inventory;
            default -> throw new AssertionError(name);
        });
        Entity cushion = proxy(Entity.class, (name, values) -> switch (name) {
            case "isValid" -> valid[0];
            case "getType" -> type[0];
            case "getPersistentDataContainer" -> container;
            case "getLocation" -> position[0].clone();
            case "getWorld" -> world;
            case "getHeight" -> 0.25;
            case "teleport" -> {
                if (teleportSucceeds[0]) {
                    position[0] = ((Location) values[0]).clone();
                    moves[0]++;
                }
                yield teleportSucceeds[0];
            }
            default -> throw new AssertionError(name);
        });
        CushionListener listener = new CushionListener();
        var adjust = CushionListener.class.getDeclaredMethod("adjust", Player.class, Entity.class, double.class);
        adjust.setAccessible(true);
        adjust.invoke(listener, player, cushion, 1.0 / 16);
        assert moves[0] == 1 && position[0].getY() == 65.0625;
        assert position[0].getX() == 10.5 && position[0].getZ() == 20.5 && position[0].getYaw() == 90;
        adjust.invoke(listener, player, cushion, 1.0 / 16);
        assert moves[0] == 1; // 同 tick 的 interact_at/interact 不重复升高
        for (int i = 0; i < 5; i++) {
            tick[0] += 4;
            adjust.invoke(listener, player, cushion, 1.0 / 16);
        }
        assert moves[0] == 6 && position[0].getY() == 65.375;
        tick[0]++;
        adjust.invoke(listener, player, cushion, -1.0 / 16);
        assert moves[0] == 7 && position[0].getY() == 65.3125;
        for (String permission : new String[]{"interact", "break", "place"}) {
            denied[0] = "buildercore.protect." + permission;
            tick[0]++;
            adjust.invoke(listener, player, cushion, 1.0 / 16);
            assert moves[0] == 7 : permission;
        }
        denied[0] = "";
        valid[0] = false;
        adjust.invoke(listener, player, cushion, 1.0 / 16);
        assert moves[0] == 7;
        valid[0] = true;
        position[0].setY(-64);
        adjust.invoke(listener, player, cushion, -1.0 / 16);
        assert moves[0] == 7;
        position[0].setY(319.75);
        adjust.invoke(listener, player, cushion, 1.0 / 16);
        assert moves[0] == 7;
        position[0].setY(65);
        teleportSucceeds[0] = false;
        adjust.invoke(listener, player, cushion, 1.0 / 16);
        assert moves[0] == 7 && position[0].getY() == 65;
        teleportSucceeds[0] = true;
        adjust.invoke(listener, player, cushion, 1.0 / 16);
        assert moves[0] == 8; // 失败不占用当前 tick
        listener.onQuit(new PlayerQuitEvent(player, (String) null));
        adjust.invoke(listener, player, cushion, 1.0 / 16);
        assert moves[0] == 9; // 离线时清理记录
        if (args.length > 0) {
            tick[0]++;
            var right = new PlayerInteractAtEntityEvent(player, cushion, new Vector(), EquipmentSlot.HAND);
            listener.onInteractAt(right);
            assert right.isCancelled() && moves[0] == 10;
            listener.onInteract(new PlayerInteractEntityEvent(player, cushion, EquipmentSlot.HAND));
            assert moves[0] == 10;
            tick[0]++;
            var offhand = new PlayerInteractEntityEvent(player, cushion, EquipmentSlot.OFF_HAND);
            listener.onInteract(offhand);
            assert offhand.isCancelled() && moves[0] == 10;
            double y = position[0].getY();
            var onBreak = CushionListener.class.getDeclaredMethod("onBreak", Entity.class, String.class,
                    Entity.class, Cancellable.class);
            onBreak.setAccessible(true);
            // 使用 26.3 的真实拆除事件，模拟通用攻击事件触发前的坐垫拆除。
            Class<?> breakType = Class.forName("io.papermc.paper.event.entity.EntityBreakByEntityEvent");
            var constructor = breakType.getConstructor(Entity.class, Entity.class,
                    Class.forName("org.bukkit.damage.DamageSource"));
            Cancellable attack = (Cancellable) constructor.newInstance(cushion, player, null);
            data.clear(); // 首次左键也必须能拦截，不依赖已有升降标记
            onBreak.invoke(listener, cushion, "ENTITY", player, attack);
            assert attack.isCancelled() && position[0].getY() == y - 1.0 / 16;
            var duplicate = (Cancellable) constructor.newInstance(cushion, player, null);
            onBreak.invoke(listener, cushion, "ENTITY", player, duplicate);
            assert duplicate.isCancelled() && position[0].getY() == y - 1.0 / 16;
            tick[0]++;
            var protectedAttack = (Cancellable) constructor.newInstance(cushion, player, null);
            protectedAttack.setCancelled(true);
            onBreak.invoke(listener, cushion, "ENTITY", player, protectedAttack);
            assert position[0].getY() == y - 1.0 / 16;
            denied[0] = "buildercore.protect.place";
            var deniedAttack = (Cancellable) constructor.newInstance(cushion, player, null);
            onBreak.invoke(listener, cushion, "ENTITY", player, deniedAttack);
            assert deniedAttack.isCancelled() && position[0].getY() == y - 1.0 / 16;
            denied[0] = "";
            var matches = CushionListener.class.getDeclaredMethod("isAdjusting", Player.class, Entity.class);
            matches.setAccessible(true);
            sneaking[0] = false;
            assert !(boolean) matches.invoke(listener, player, cushion);
            var normalAttack = (Cancellable) constructor.newInstance(cushion, player, null);
            onBreak.invoke(listener, cushion, "ENTITY", player, normalAttack);
            assert !normalAttack.isCancelled();
            sneaking[0] = true;
            mode[0] = GameMode.SPECTATOR;
            assert !(boolean) matches.invoke(listener, player, cushion);
            mode[0] = GameMode.CREATIVE;
            item.type = Material.STICK;
            assert !(boolean) matches.invoke(listener, player, cushion);
            item.type = Material.BREEZE_ROD;
            for (String cause : new String[]{"PHYSICS", "OBSTRUCTION", "ENTITY", "EXPLOSION", "DEFAULT"}) {
                var event = new PlayerInteractEntityEvent(player, cushion);
                onBreak.invoke(listener, cushion, cause, null, event);
                assert event.isCancelled() == (cause.equals("PHYSICS") || cause.equals("OBSTRUCTION")) : cause;
            }
            data.clear();
            var unedited = new PlayerInteractEntityEvent(player, cushion);
            onBreak.invoke(listener, cushion, "PHYSICS", null, unedited);
            assert !unedited.isCancelled();
            type[0] = EntityType.ARMOR_STAND;
            assert !(boolean) matches.invoke(listener, player, cushion);
            System.out.println("CushionListener 26.3 interaction and physics checks passed");
        }
        System.out.println("CushionListener movement checks passed");
    }
}
