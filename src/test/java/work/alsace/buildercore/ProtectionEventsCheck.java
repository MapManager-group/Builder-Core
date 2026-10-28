package work.alsace.buildercore;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** 在隔离 Paper 服务端调用 run；使用真实世界、物品、实体和已注册的事件链。 */
public final class ProtectionEventsCheck {
    public static void run(BuilderCore core) {
        World world = Bukkit.getWorlds().getFirst();
        UUID id = UUID.randomUUID();
        AtomicReference<Location> eye = new AtomicReference<>(new Location(world, .5, 101.62, .5));
        java.util.concurrent.atomic.AtomicInteger opens = new java.util.concurrent.atomic.AtomicInteger();
        AtomicBoolean permission = new AtomicBoolean(true);
        AtomicReference<ItemStack> hand = new AtomicReference<>(new ItemStack(Material.STONE));
        PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(), new Class<?>[]{PlayerInventory.class},
                (p, method, args) -> switch (method.getName()) {
                    case "getItemInMainHand" -> hand.get();
                    default -> throw new AssertionError(method.getName());
                });
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (p, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "getName" -> "ProtectionCheck";
                    case "hasPermission" -> permission.get();
                    case "getGameMode" -> GameMode.CREATIVE;
                    case "getType" -> org.bukkit.entity.EntityType.PLAYER;
                    case "getWorld" -> world;
                    case "getInventory" -> inventory;
                    case "getLocation" -> eye.get().clone().subtract(0, 1.62, 0);
                    case "getEyeLocation" -> eye.get().clone();
                    case "sendMessage", "playSound", "isSneaking" -> method.getReturnType() == boolean.class ? false : null;
                    case "openInventory" -> {
                        opens.incrementAndGet();
                        yield Proxy.newProxyInstance(org.bukkit.inventory.InventoryView.class.getClassLoader(), new Class<?>[]{org.bukkit.inventory.InventoryView.class}, (view, called, arguments) -> null);
                    }
                    case "hashCode" -> id.hashCode();
                    case "equals" -> p == args[0];
                    default -> throw new AssertionError(method.getName());
                });
        Block light = world.getBlockAt(0, 100, 0);
        var oldBlock = light.getState();
        Cow cow = world.spawn(new Location(world, 2.5, 100, .5), Cow.class);
        cow.setAI(false);
        cow.setInvulnerable(true);
        try {
            light.setType(Material.LIGHT, false);
            int initial = ((Levelled) light.getBlockData()).getLevel();
            permission.set(false);
            fire(click(player, light, EquipmentSlot.HAND));
            assert ((Levelled) light.getBlockData()).getLevel() == initial : "light edit without permission";
            permission.set(true);
            PlayerInteractEvent denied = click(player, light, EquipmentSlot.HAND);
            denied.setUseItemInHand(Event.Result.DENY);
            fire(denied);
            assert ((Levelled) light.getBlockData()).getLevel() == initial : "cancelled light edit";
            fire(click(player, light, EquipmentSlot.OFF_HAND));
            assert ((Levelled) light.getBlockData()).getLevel() == initial : "offhand duplicate";
            fire(click(player, light, EquipmentSlot.HAND));
            assert ((Levelled) light.getBlockData()).getLevel() == (initial + 1) % 16 : "permitted light edit";

            org.bukkit.event.Listener lateProtection = new work.alsace.buildercore.listeners.BlockListener();
            Bukkit.getPluginManager().registerEvent(PlayerInteractEvent.class, lateProtection, org.bukkit.event.EventPriority.HIGHEST,
                    (listener, event) -> ((PlayerInteractEvent) event).setCancelled(true), core);
            try {
                int before = ((Levelled) light.getBlockData()).getLevel();
                fire(click(player, light, EquipmentSlot.HAND));
                assert ((Levelled) light.getBlockData()).getLevel() == before : "later HIGHEST protection must stop light edits";
            } finally { org.bukkit.event.HandlerList.unregisterAll(lateProtection); }

            hand.set(new ItemStack(Material.STONE));
            int entitiesBefore = world.getLivingEntities().size();
            var egg = new ItemStack(Material.COW_SPAWN_EGG);
            fire(new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, egg, light, org.bukkit.block.BlockFace.UP, EquipmentSlot.OFF_HAND));
            assert world.getLivingEntities().size() == entitiesBefore + 1 : "offhand egg remains usable";
            for (var entity : world.getLivingEntities()) if (entity instanceof Cow && entity != cow) entity.remove();
            var chest = new work.alsace.buildercore.listeners.ChestListener();
            chest.onPlayerRightClickAir(new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, new ItemStack(Material.SHULKER_BOX), null,
                    org.bukkit.block.BlockFace.SELF, EquipmentSlot.OFF_HAND));
            assert opens.get() == 1 : "offhand shulker preview remains usable";

            light.setType(Material.STONE_SLAB, false);
            var doubleSlab = (org.bukkit.block.data.type.Slab) light.getBlockData();
            doubleSlab.setType(org.bukkit.block.data.type.Slab.Type.DOUBLE);
            light.setBlockData(doubleSlab, false);
            hand.set(new ItemStack(Material.STONE_SLAB));
            eye.set(new Location(world, .5, 110.75, -10.5, 0, 45));
            work.alsace.buildercore.listeners.BlockListener.slabs.add(player);
            Bukkit.getPluginManager().registerEvent(org.bukkit.event.block.BlockBreakEvent.class, lateProtection, org.bukkit.event.EventPriority.HIGHEST,
                    (listener, event) -> ((org.bukkit.event.block.BlockBreakEvent) event).setCancelled(true), core);
            try {
                fire(new org.bukkit.event.block.BlockBreakEvent(light, player));
                assert ((org.bukkit.block.data.type.Slab) light.getBlockData()).getType() == org.bukkit.block.data.type.Slab.Type.DOUBLE;
            } finally { org.bukkit.event.HandlerList.unregisterAll(lateProtection); }
            fire(new org.bukkit.event.block.BlockBreakEvent(light, player));
            assert ((org.bukkit.block.data.type.Slab) light.getBlockData()).getType() == org.bukkit.block.data.type.Slab.Type.TOP
                    : "long-reach ray must remove the targeted lower half";
            work.alsace.buildercore.listeners.BlockListener.slabs.remove(player);
            eye.set(new Location(world, .5, 101.62, .5));

            hand.set(new ItemStack(Material.DIAMOND_SWORD));
            fire(attack(player, cow));
            fire(attack(player, cow));
            assert !cow.isDead() : "sword/protection cancellation must prevent direct entity removal";
            hand.set(new ItemStack(Material.STONE));
            fire(attack(player, cow));
            assert !cow.isDead();
            fire(attack(player, cow));
            assert cow.isDead() : "two valid clicks must still remove a mob";
            fire(attack(player, player));
            fire(attack(player, player));
        } finally {
            work.alsace.buildercore.listeners.BlockListener.slabs.remove(player);
            cow.remove();
            oldBlock.update(true, false);
        }
        core.getLogger().info("ProtectionEventsCheck passed: actual light edits, hand/cancellation guards and two-click mob removal");
    }

    private static PlayerInteractEvent click(Player player, Block block, EquipmentSlot hand) {
        return new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, new ItemStack(Material.STONE), block,
                org.bukkit.block.BlockFace.UP, hand);
    }

    private static EntityDamageByEntityEvent attack(Player player, org.bukkit.entity.Entity target) {
        return new EntityDamageByEntityEvent(player, target, EntityDamageEvent.DamageCause.ENTITY_ATTACK, 1);
    }

    private static void fire(Event event) { Bukkit.getPluginManager().callEvent(event); }
}
