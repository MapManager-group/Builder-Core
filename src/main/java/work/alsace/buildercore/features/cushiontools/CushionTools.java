package work.alsace.buildercore.features.cushiontools;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Colorable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public final class CushionTools implements Listener, CommandExecutor {
    private static final NamespacedKey TOOL = new NamespacedKey("buildercore", "cushion_tool");
    private static final String[] NAMES = {"X 轴微调", "Y 轴微调", "Z 轴微调", "准星拖动", "连续放置", "连续拆除", "两点框选", "吸取样板", "坐垫工具菜单"};
    private static final Material[] MATERIALS = {Material.RED_DYE, Material.LIME_DYE, Material.BLUE_DYE, Material.LEAD,
            Material.BRICK, Material.SHEARS, Material.BLAZE_ROD, Material.GLASS_BOTTLE, Material.CHEST};
    private static final String[] COLORS = {"白色", "橙色", "品红色", "淡蓝色", "黄色", "黄绿色", "粉红色", "灰色", "淡灰色", "青色", "紫色", "蓝色", "棕色", "绿色", "红色", "黑色"};
    private final JavaPlugin plugin;
    private final InventoryBackup backups;
    private final CushionAccess access;
    private final Map<UUID, CushionSession> sessions = new HashMap<>();
    private boolean available;

    public CushionTools(JavaPlugin plugin) {
        this.plugin = plugin;
        backups = new InventoryBackup(plugin.getDataFolder().toPath().resolve("cushiontools/inventories"));
        access = new CushionAccess(plugin);
    }

    public void register() {
        available = access.register(this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        Objects.requireNonNull(plugin.getCommand("cushiontools")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("cushiontools")).setTabCompleter((s, c, l, a) -> List.of());
        for (Player player : plugin.getServer().getOnlinePlayers()) recover(player);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateDrags, 1, 1);
    }

    public void close() {
        for (Player player : plugin.getServer().getOnlinePlayers()) if (active(player)) restore(player);
    }

    boolean active(Player player) { return sessions.containsKey(player.getUniqueId()); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (work.alsace.buildercore.commands.CommandHelp.requested(sender, command, label, args)) return true;
        if (!(sender instanceof Player player)) { sender.sendMessage("仅玩家可使用此命令"); return true; }
        if (active(player) || backups.exists(player.getUniqueId())) { restore(player); return true; }
        if (!available) { tell(player, "坐垫工具未能初始化，请查看服务器启动日志中的 API 检测错误。"); return true; }
        if (!CushionAccess.permitted(player)) { tell(player, "需要坐垫工具权限及放置、拆除、交互权限，旁观模式不可使用。"); return true; }
        if (player.isDead()) return true;
        if (!player.getItemOnCursor().getType().isAir()) { tell(player, "请先放下光标上的物品。"); return true; }
        player.closeInventory();
        try {
            backups.save(player);
            sessions.put(player.getUniqueId(), new CushionSession());
            player.getInventory().clear();
            for (int slot = 0; slot < NAMES.length; slot++) {
                ItemStack item = item(MATERIALS[slot], NAMES[slot]);
                ItemMeta meta = item.getItemMeta();
                meta.getPersistentDataContainer().set(TOOL, PersistentDataType.INTEGER, slot);
                meta.lore(List.of(Component.text(slot < 3 ? "左键减少 / 右键增加 1/32 格" : switch (slot) {
                    case 3 -> "右键选中 / 再右键落位；左键取消";
                    case 4 -> "右键地面放置；沿途点击刷放";
                    case 5 -> "点击坐垫拆除；不掉落物品";
                    case 6 -> "左键第一点 / 右键第二点";
                    case 7 -> "点击坐垫吸取颜色和朝向";
                    default -> "右键打开颜色、朝向与批量操作菜单";
                })));
                item.setItemMeta(meta);
                player.getInventory().setItem(slot, item);
            }
            player.getInventory().setHeldItemSlot(0);
            player.saveData();
            tell(player, "已进入坐垫工具模式。快捷栏选择工具；再次 /cushiontools 恢复原背包。");
        } catch (IOException | RuntimeException exception) {
            failure(player, exception);
            if (backups.exists(player.getUniqueId())) recover(player);
        }
        return true;
    }

    private void recover(Player player) {
        if (!backups.exists(player.getUniqueId())) return;
        sessions.putIfAbsent(player.getUniqueId(), new CushionSession());
        sessions.get(player.getUniqueId()).recovering = true;
        restore(player);
    }

    private void restore(Player player) {
        CushionSession state = sessions.get(player.getUniqueId());
        if (state != null) { stopDrag(player, state, true); state.pending = null; }
        try {
            backups.restore(player);
            sessions.remove(player.getUniqueId());
            tell(player, "原背包已恢复。");
        } catch (IOException | RuntimeException exception) {
            sessions.putIfAbsent(player.getUniqueId(), new CushionSession());
            sessions.get(player.getUniqueId()).recovering = true;
            failure(player, exception);
        }
    }

    private void failure(Player player, Exception exception) {
        plugin.getLogger().log(Level.SEVERE, "坐垫工具背包保存/恢复失败：" + player.getUniqueId(), exception);
        tell(player, "背包操作失败，已保留原备份。请再次 /cushiontools 重试并联系管理员，勿删除备份文件。");
    }

    private static void tell(Player player, String text) { player.sendMessage(Component.text(text)); }
    private static ItemStack item(Material type, String name) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        item.setItemMeta(meta);
        return item;
    }

    private int tool(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        return item.hasItemMeta() ? item.getItemMeta().getPersistentDataContainer().getOrDefault(TOOL, PersistentDataType.INTEGER, -1) : -1;
    }

    private boolean once(Player player, CushionSession state) {
        if (state.lastTick == player.getTicksLived()) return false;
        state.lastTick = player.getTicksLived();
        return true;
    }

    void use(Player player, Entity cushion, boolean right) {
        CushionSession state = sessions.get(player.getUniqueId());
        if (state == null || state.recovering || state.running || !CushionAccess.permitted(player) || !once(player, state)) return;
        int tool = tool(player);
        if (tool == 8) { menu(player); return; }
        if (tool == 3 && state.dragging != null) { stopDrag(player, state, !right); return; }
        if (!CushionAccess.isCushion(cushion) || !cushion.isValid()) return;
        if (tool >= 0 && tool <= 2) {
            double delta = right ? CushionSession.STEP : -CushionSession.STEP;
            Location position = cushion.getLocation().add(tool == 0 ? delta : 0, tool == 1 ? delta : 0, tool == 2 ? delta : 0);
            if (!access.move(player, cushion, position)) tell(player, "移动失败：请检查乘客、权限、区块及世界边界。");
        } else if (tool == 3 && right && cushion.isEmpty()) {
            state.dragging = cushion;
            state.dragOrigin = cushion.getLocation();
            state.dragDistance = Math.max(1, Math.min(8, player.getEyeLocation().distance(state.dragOrigin)));
            CushionAccess.mark(cushion);
            tell(player, "已选中坐垫，移动准星拖动；右键落位，左键取消。");
        } else if (tool == 5) {
            if (!access.remove(player, cushion)) tell(player, "拆除失败：可能被保护插件阻止，或坐垫上有乘客。");
        } else if (tool == 7) {
            state.color = ((Colorable) cushion).getColor();
            state.yaw = cushion.getLocation().getYaw();
            state.pending = null;
            tell(player, "已吸取坐垫颜色和朝向。");
        }
    }

    private Entity target(Player player) {
        Location eye = player.getEyeLocation();
        RayTraceResult block = player.getWorld().rayTraceBlocks(eye, eye.getDirection(), 8, FluidCollisionMode.NEVER, true);
        double distance = block == null ? 8 : Math.min(8, block.getHitPosition().distance(eye.toVector()) + 0.3);
        RayTraceResult hit = player.getWorld().rayTraceEntities(eye, eye.getDirection(), distance, 0.1, CushionAccess::isCushion);
        return hit == null ? null : hit.getHitEntity();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void interact(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!active(player)) return;
        boolean blocked = event.getClickedBlock() != null && event.useItemInHand() == Event.Result.DENY;
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND || event.getAction().name().equals("PHYSICAL")) return;
        CushionSession state = sessions.get(player.getUniqueId());
        String unavailable = blocked ? "此处的工具交互已被保护规则拦截。"
                : state.recovering ? "原背包等待恢复，请使用 /cushiontools 重试。"
                : state.running ? "批量操作正在执行，请稍候。"
                : !CushionAccess.permitted(player) ? "需要坐垫工具、放置、拆除和交互权限，旁观模式不可操作。" : null;
        if (unavailable != null) {
            player.sendActionBar(Component.text(unavailable));
            return;
        }
        boolean right = event.getAction().name().startsWith("RIGHT");
        int tool = tool(player);
        if (tool < 0 || tool >= NAMES.length) {
            player.sendActionBar(Component.text("请手持快捷栏中的坐垫工具；异常时可退出后重新进入工具模式。"));
            return;
        }
        if (tool == 6 && event.getClickedBlock() != null) {
            if (!once(player, state)) return;
            Location point = event.getClickedBlock().getLocation();
            if (right) state.second = point; else { state.first = point; state.second = null; }
            state.pending = null;
            tell(player, (right ? "第二点" : "第一点") + "：" + point.getBlockX() + ", " + point.getBlockY() + ", " + point.getBlockZ());
        } else if (tool == 4 && right && event.getClickedBlock() != null) {
            if (!once(player, state)) return;
            Block block = event.getClickedBlock();
            Location location = ground(block.getWorld(), block.getX(), block.getZ(), block.getY());
            if (location == null || !access.place(player, location, state.color, state.yaw)) tell(player, "此处无法放置坐垫：检查地面、重复坐垫及保护权限。");
        } else {
            use(player, target(player), right);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void entityInteract(PlayerInteractEntityEvent event) {
        if (!active(event.getPlayer())) return;
        boolean blocked = event.isCancelled();
        event.setCancelled(true);
        if (!blocked && event.getHand() == EquipmentSlot.HAND) use(event.getPlayer(), event.getRightClicked(), true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void entityInteractAt(PlayerInteractAtEntityEvent event) { entityInteract(event); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void attack(PrePlayerAttackEntityEvent event) {
        if (!active(event.getPlayer())) return;
        boolean allowed = !event.isCancelled();
        event.setCancelled(true);
        if (allowed) use(event.getPlayer(), event.getAttacked(), false);
    }

    private void updateDrags() {
        for (var entry : sessions.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            CushionSession state = entry.getValue();
            if (player == null || state.dragging == null) continue;
            if (!state.dragging.isValid() || !CushionAccess.permitted(player)
                    || !player.getWorld().equals(state.dragOrigin.getWorld()) || tool(player) != 3) {
                stopDrag(player, state, true);
                continue;
            }
            Location eye = player.getEyeLocation();
            Location destination = eye.add(eye.getDirection().multiply(state.dragDistance));
            destination.setYaw(state.dragOrigin.getYaw());
            destination.setPitch(state.dragOrigin.getPitch());
            access.move(player, state.dragging, destination);
        }
    }

    private void stopDrag(Player player, CushionSession state, boolean cancel) {
        if (state.dragging == null) return;
        if (cancel && state.dragging.isValid() && !access.rollback(state.dragging, state.dragOrigin))
            tell(player, "无法退回拖动起点，坐垫保留在最后有效位置。");
        state.dragging = null;
        state.dragOrigin = null;
    }

    private Location ground(World world, int x, int z, int reference) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) return null;
        for (int y : CushionSession.groundLevels(reference, world.getMinHeight(), world.getMaxHeight() - 1)) {
            Block block = world.getBlockAt(x, y, z);
            if (block.isLiquid() || block.isPassable()) continue;
            RayTraceResult surface = block.rayTrace(new Location(world, x + .5, y + 1.01, z + .5), new Vector(0, -1, 0), 1.02, FluidCollisionMode.NEVER);
            if (surface == null || surface.getHitBlockFace() != org.bukkit.block.BlockFace.UP) continue;
            Location location = surface.getHitPosition().toLocation(world);
            if (!CushionAccess.loaded(location)) continue;
            if (world.rayTraceBlocks(location.clone().add(0, .001, 0), new Vector(0, 1, 0), .26, FluidCollisionMode.ALWAYS, false) == null)
                return location;
        }
        return null;
    }

    private static final class ToolMenu implements InventoryHolder {
        private final Inventory inventory = Bukkit.createInventory(this, 36, Component.text("坐垫工具"));
        @Override public Inventory getInventory() { return inventory; }
    }

    private void menu(Player player) {
        CushionSession state = sessions.get(player.getUniqueId());
        if (state == null) return;
        ToolMenu holder = new ToolMenu();
        Inventory inventory = holder.getInventory();
        for (int i = 0; i < DyeColor.values().length; i++) {
            DyeColor color = DyeColor.values()[i];
            inventory.setItem(i, item(Material.valueOf(color.name() + "_WOOL"), COLORS[i] + (state.color == color ? "（当前）" : "")));
        }
        String[] directions = {"朝南", "朝西", "朝北", "朝东"};
        for (int i = 0; i < 4; i++) inventory.setItem(18 + i, item(Material.COMPASS, directions[i]));
        inventory.setItem(23, item(Material.PAPER, "当前朝向：" + state.yaw + "°"));
        inventory.setItem(27, item(Material.BRICKS, "预览选区批量放置"));
        inventory.setItem(28, item(Material.TNT, "预览选区批量拆除"));
        inventory.setItem(30, item(Material.LIME_CONCRETE, state.pending == null ? "请先预览操作" : "确认执行：" + state.pending.size() + " 个坐垫"));
        inventory.setItem(31, item(Material.RED_CONCRETE, "取消待执行操作"));
        inventory.setItem(35, item(Material.BARRIER, "退出工具并恢复背包"));
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void inventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !active(player)) return;
        event.setCancelled(true);
        if (!(event.getView().getTopInventory().getHolder() instanceof ToolMenu)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 36) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            CushionSession state = sessions.get(player.getUniqueId());
            if (state == null || !player.isOnline()) return;
            if (slot == 35) { restore(player); return; }
            if (state.recovering || state.running || !CushionAccess.permitted(player)) return;
            if (slot < 16) { state.color = DyeColor.values()[slot]; state.pending = null; }
            else if (slot >= 18 && slot <= 21) { state.yaw = (slot - 18) * 90; state.pending = null; }
            else if (slot == 27 || slot == 28) { preview(player, state, slot == 28); }
            else if (slot == 30) { execute(player, state); return; }
            else if (slot == 31) state.pending = null;
            menu(player);
        });
    }

    private void preview(Player player, CushionSession state, boolean removal) {
        state.pending = null;
        stopDrag(player, state, false);
        Location first = state.first, second = state.second;
        if (first == null || second == null || !first.getWorld().equals(second.getWorld()) || !first.getWorld().equals(player.getWorld())) {
            tell(player, "请先用框选工具在当前世界左键、右键选取两个角。"); return;
        }
        if (CushionSession.area(first, second) > CushionSession.LIMIT) { tell(player, "选区水平面积上限为 1024 格，请缩小选区。"); return; }
        List<Location> placements = new ArrayList<>();
        List<UUID> removals = new ArrayList<>();
        World world = player.getWorld();
        int minX = Math.min(first.getBlockX(), second.getBlockX()), maxX = Math.max(first.getBlockX(), second.getBlockX());
        int minZ = Math.min(first.getBlockZ(), second.getBlockZ()), maxZ = Math.max(first.getBlockZ(), second.getBlockZ());
        for (int x = minX >> 4; x <= maxX >> 4; x++) for (int z = minZ >> 4; z <= maxZ >> 4; z++)
            if (!world.isChunkLoaded(x, z)) { tell(player, "选区包含未加载区块，请靠近后重试。"); return; }
        if (removal) {
            BoundingBox bounds = new BoundingBox(minX, Math.min(first.getBlockY(), second.getBlockY()), minZ,
                    maxX + 1.0, Math.max(first.getBlockY(), second.getBlockY()) + 2.0, maxZ + 1.0);
            for (Entity entity : world.getNearbyEntities(bounds, CushionAccess::isCushion)) {
                if (entity.isEmpty() && CushionSession.withinSelection(entity.getLocation(), first, second)) removals.add(entity.getUniqueId());
                if (removals.size() > CushionSession.LIMIT) { tell(player, "坐垫超过 1024 个，请缩小选区。"); return; }
            }
        } else {
            for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
                Location location = ground(world, x, z, first.getBlockY());
                if (location != null && world.getNearbyEntities(location, .45, .2, .45, CushionAccess::isCushion).isEmpty()) placements.add(location);
            }
        }
        state.pending = new CushionSession.Plan(world.getUID(), placements, removals, state.color, state.yaw);
        tell(player, (removal ? "待拆除" : "待放置") + "：" + state.pending.size() + " 个坐垫。点击绿色按钮确认；红色按钮取消。");
    }

    private void execute(Player player, CushionSession state) {
        CushionSession.Plan plan = state.pending;
        if (plan == null || plan.size() == 0) { tell(player, "没有可执行的操作，请先预览。"); return; }
        state.pending = null;
        Location first = state.first.clone(), second = state.second.clone();
        state.running = true;
        player.closeInventory();
        new BukkitRunnable() {
            int index, success;
            @Override public void run() {
                if (!player.isOnline() || state.recovering || sessions.get(player.getUniqueId()) != state || !CushionAccess.permitted(player)
                        || !player.getWorld().getUID().equals(plan.world())) { finish("操作已中止"); return; }
                try {
                    for (int n = 0; n < 32 && index < plan.size(); n++, index++) {
                        if (!plan.placements().isEmpty()) {
                            Location location = plan.placements().get(index);
                            Location current = ground(player.getWorld(), location.getBlockX(), location.getBlockZ(), first.getBlockY());
                            if (current != null && current.distanceSquared(location) < .0001
                                    && access.place(player, location, plan.color(), plan.yaw())) success++;
                        } else {
                            Entity entity = plugin.getServer().getEntity(plan.removals().get(index));
                            if (entity != null && CushionSession.withinSelection(entity.getLocation(), first, second)
                                    && access.remove(player, entity)) success++;
                        }
                    }
                    if (index >= plan.size()) finish("操作完成");
                } catch (RuntimeException exception) {
                    plugin.getLogger().log(Level.SEVERE, "坐垫批量操作失败", exception);
                    finish("操作出错，已停止");
                }
            }
            private void finish(String message) {
                state.running = false;
                cancel();
                if (player.isOnline()) tell(player, message + "：成功 " + success + " / " + plan.size() + "；其余跳过或尚未执行。");
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    @EventHandler public void join(PlayerJoinEvent event) { recover(event.getPlayer()); }
    @EventHandler public void quit(PlayerQuitEvent event) {
        if (active(event.getPlayer())) restore(event.getPlayer());
        sessions.remove(event.getPlayer().getUniqueId());
    }
    @EventHandler public void respawn(PlayerPostRespawnEvent event) { if (active(event.getPlayer())) restore(event.getPlayer()); }
    @EventHandler(priority = EventPriority.HIGHEST) public void death(PlayerDeathEvent event) {
        if (!active(event.getEntity())) return;
        event.getDrops().clear();
        event.setKeepInventory(true);
        event.setKeepLevel(true);
    }
    @EventHandler public void worldChange(PlayerChangedWorldEvent event) {
        CushionSession state = sessions.get(event.getPlayer().getUniqueId());
        if (state != null) { stopDrag(event.getPlayer(), state, true); state.first = null; state.second = null; state.pending = null; }
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void commands(PlayerCommandPreprocessEvent event) {
        if (!active(event.getPlayer())) return;
        String name = event.getMessage().split(" ", 2)[0];
        if (!name.equalsIgnoreCase("/cushiontools") && !name.equalsIgnoreCase("/buildercore:cushiontools")) {
            event.setCancelled(true);
            tell(event.getPlayer(), "请先 /cushiontools 退出工具模式，再执行其他命令。");
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void inventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player && active(player) && !(event.getInventory().getHolder() instanceof ToolMenu)) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void inventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && active(player)) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void drop(PlayerDropItemEvent event) { if (active(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void pickup(EntityPickupItemEvent event) { if (event.getEntity() instanceof Player player && active(player)) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void swap(PlayerSwapHandItemsEvent event) { if (active(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void blockBreak(BlockBreakEvent event) { if (active(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void blockPlace(BlockPlaceEvent event) { if (active(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void damage(EntityDamageByEntityEvent event) { if (event.getDamager() instanceof Player player && active(player)) event.setCancelled(true); }
}
