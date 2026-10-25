package work.alsace.buildercore.features;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;
import work.alsace.buildercore.commands.builderTools.AdvanceFlyCommand;
import work.alsace.buildercore.listeners.BlockListener;
import work.alsace.buildercore.utils.NoClipUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class BuilderTools implements Listener, CommandExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final JavaPlugin plugin;
    private final Map<UUID, Map<String, Boolean>> abilities = new HashMap<>();
    private final Map<UUID, BannerSession> banners = new HashMap<>();
    private final Map<UUID, int[]> colors = new HashMap<>();

    public BuilderTools(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getPluginManager().registerEvents(new BuildingSettings(plugin, this), plugin);
        for (String name : List.of("buildertools", "armorcolor", "banner")) {
            if (plugin.getCommand(name) != null) plugin.getCommand(name).setExecutor(this);
        }
        if (plugin.getConfig().getBoolean("worldedit-extra-aliases", true)) {
            for (String name : List.of("/p1", "/p2", "/pa", "/s", "/r", "/f", "/c")) {
                if (plugin.getCommand(name) != null) plugin.getCommand(name).setExecutor(this);
            }
        }
    }

    boolean isEnabled(Player player, String ability) {
        return abilities.getOrDefault(player.getUniqueId(), Map.of()).getOrDefault(ability, true);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("仅玩家可使用此命令");
            return true;
        }
        if (command.getName().startsWith("/")) {
            if (!player.hasPermission("buildercore.aliases")) {
                player.sendMessage(MM.deserialize("<red>你没有使用该命令的权限"));
                return true;
            }
            String target = switch (command.getName()) {
                case "/p1" -> "/pos1"; case "/p2" -> "/pos2"; case "/pa" -> "/paste";
                case "/s" -> "/set"; case "/r" -> "/replace";
                case "/f" -> "/flip"; case "/c" -> "/copy";
                default -> null;
            };
            if (target != null) player.performCommand(target + (args.length == 0 ? "" : " " + String.join(" ", args)));
            return true;
        }
        String mode = command.getName().equalsIgnoreCase("buildertools")
                ? (args.length == 0 ? "ability" : args[0].toLowerCase()) : command.getName().toLowerCase();
        String permission = switch (mode) {
            case "ability" -> "buildercore.menu.ability";
            case "armorcolor" -> "buildercore.menu.armorcolor";
            case "banner" -> "buildercore.menu.banner";
            case "special" -> "buildercore.menu.special";
            case "rc" -> "buildercore.commands.rc";
            default -> "";
        };
        if (permission.isEmpty()) {
            player.sendMessage(MM.deserialize("<gray>用法: /buildertools [ability|armorcolor|banner|special|rc]"));
            return true;
        }
        if (!player.hasPermission(permission)) {
            player.sendMessage(MM.deserialize("<red>你没有使用该功能的权限"));
            return true;
        }
        switch (mode) {
            case "ability" -> showAbility(player);
            case "armorcolor" -> showArmor(player);
            case "banner" -> showBanner(player);
            case "special" -> showSpecial(player);
            case "rc" -> refreshChunks(player);
        }
        return true;
    }

    private void refreshChunks(Player player) {
        int radius = Math.min(8, Math.min(player.getClientViewDistance(), player.getWorld().getViewDistance()));
        int centerX = player.getLocation().getBlockX() >> 4;
        int centerZ = player.getLocation().getBlockZ() >> 4;
        int refreshed = 0;
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                if (player.getWorld().isChunkLoaded(x, z) && player.getWorld().refreshChunk(x, z)) refreshed++;
            }
        }
        player.sendMessage(MM.deserialize("<gray>已刷新 " + refreshed + " 个已加载区块"));
    }

    private Inventory menu(String kind, int size, String title) {
        MenuHolder holder = new MenuHolder(kind);
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.inventory = inventory;
        return inventory;
    }

    private ItemStack item(Material material, String name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(MM.deserialize("<reset>" + name));
        stack.setItemMeta(meta);
        return stack;
    }

    private void showAbility(Player player) {
        Inventory inventory = menu("ability", 27, "建筑能力");
        String[] names = {"铁门开关", "釉面陶瓦旋转", "双层半砖破坏", "夜视", "穿墙", "进阶飞行"};
        String[] keys = {"iron-door", "terracotta", "slab", "night", "noclip", "advfly"};
        Material[] icons = {Material.IRON_TRAPDOOR, Material.LIGHT_BLUE_GLAZED_TERRACOTTA,
                Material.STONE_SLAB, Material.ENDER_EYE, Material.COMPASS, Material.FEATHER};
        for (int i = 0; i < keys.length; i++) {
            boolean enabled = switch (keys[i]) {
                case "slab" -> BlockListener.slabs.contains(player);
                case "night" -> player.hasPotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION);
                case "noclip" -> NoClipUtil.noclip.contains(player);
                case "advfly" -> AdvanceFlyCommand.enabledPlayers.contains(player);
                default -> isEnabled(player, keys[i]);
            };
            inventory.setItem(10 + i + (i >= 3 ? 1 : 0), item(icons[i], names[i] + "：" + (enabled ? "开启" : "关闭")));
        }
        player.openInventory(inventory);
    }

    private void showArmor(Player player) {
        int[] rgb = colors.computeIfAbsent(player.getUniqueId(), ignored -> new int[]{128, 128, 128});
        Inventory inventory = menu("armor", 45, "皮革护甲配色");
        Material[] pieces = {Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE,
                Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS};
        for (int i = 0; i < pieces.length; i++) {
            ItemStack armor = item(pieces[i], "点击领取");
            LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();
            meta.setColor(Color.fromRGB(rgb[0], rgb[1], rgb[2]));
            armor.setItemMeta(meta);
            inventory.setItem(10 + i * 9, armor);
        }
        for (int i = 0; i < 3; i++) {
            inventory.setItem(31 + i, item(new Material[]{Material.RED_DYE, Material.LIME_DYE, Material.BLUE_DYE}[i],
                    new String[]{"红", "绿", "蓝"}[i] + ": " + rgb[i] + " (左键 +8，右键 -8，Shift 随机)"));
        }
        player.openInventory(inventory);
    }

    private void showBanner(Player player) {
        BannerSession session = banners.computeIfAbsent(player.getUniqueId(), ignored -> new BannerSession());
        Inventory inventory = menu("banner", 54, "旗帜编辑器");
        inventory.setItem(0, item(Material.BARRIER, "重置"));
        inventory.setItem(1, item(Material.ARROW, "撤销"));
        inventory.setItem(4, session.banner());
        if (session.patterns.size() < 16) inventory.setItem(7, item(Material.DISPENSER, "随机选择"));
        inventory.setItem(8, item(Material.CHEST, "领取旗帜"));
        if (session.base == null || session.nextColor == null && session.patterns.size() < 16) {
            int slot = 18;
            for (DyeColor color : DyeColor.values()) {
                Material material = Material.matchMaterial(color.name() + (session.base == null ? "_BANNER" : "_DYE"));
                if (material != null) inventory.setItem(slot++, item(material, color.name()));
            }
        } else if (session.patterns.size() < 16) {
            int slot = 9;
            for (PatternType pattern : PatternType.values()) {
                if (pattern == PatternType.BASE || slot >= 54) continue;
                ItemStack preview = session.banner();
                BannerMeta meta = (BannerMeta) preview.getItemMeta();
                meta.addPattern(new Pattern(session.nextColor, pattern));
                meta.displayName(MM.deserialize("<reset>" + pattern.name()));
                preview.setItemMeta(meta);
                inventory.setItem(slot++, preview);
            }
        }
        player.openInventory(inventory);
    }

    private void showSpecial(Player player) {
        List<String> configured = plugin.getConfig().getStringList("special-items");
        Inventory inventory = menu("special", Math.max(9, Math.min(54, ((configured.size() + 8) / 9) * 9)), "特殊物品");
        for (String name : configured) {
            try {
                inventory.addItem(Bukkit.getItemFactory().createItemStack(name.toLowerCase()));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("无效的特殊物品: " + name);
            }
        }
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)
                || !(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        int slot = event.getRawSlot();
        switch (holder.kind) {
            case "ability" -> clickAbility(player, slot);
            case "armor" -> clickArmor(player, slot, event.isLeftClick(), event.isShiftClick());
            case "banner" -> clickBanner(player, slot);
            case "special" -> {
                ItemStack selected = event.getCurrentItem();
                if (selected != null && selected.getType() != Material.AIR)
                    player.getInventory().addItem(selected.clone()).values().forEach(overflow -> player.getWorld().dropItemNaturally(player.getLocation(), overflow));
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        abilities.remove(id);
        banners.remove(id);
        colors.remove(id);
    }

    private void clickAbility(Player player, int slot) {
        String key = switch (slot) {
            case 10 -> "iron-door"; case 11 -> "terracotta"; case 12 -> "slab";
            case 14 -> "night"; case 15 -> "noclip"; case 16 -> "advfly";
            default -> null;
        };
        if (key == null) return;
        String permission = switch (key) {
            case "iron-door" -> "buildercore.ability.irondoor";
            case "terracotta" -> "buildercore.ability.terracotta";
            case "slab" -> "buildercore.commands.slab";
            case "night" -> "buildercore.commands.nightvision";
            case "noclip" -> "buildercore.commands.noclip";
            default -> "buildercore.commands.advfly";
        };
        if (!player.hasPermission(permission)) return;
        String command = switch (key) {
            case "slab" -> "slab"; case "night" -> "night";
            case "noclip" -> "noclip"; case "advfly" -> "advfly";
            default -> null;
        };
        if (command != null) Bukkit.dispatchCommand(player, command);
        else abilities.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>()).put(key, !isEnabled(player, key));
        Bukkit.getScheduler().runTask(plugin, () -> showAbility(player));
    }

    private void clickArmor(Player player, int slot, boolean left, boolean shift) {
        if (slot == 10 || slot == 19 || slot == 28 || slot == 37) {
            ItemStack armor = player.getOpenInventory().getTopInventory().getItem(slot);
            if (armor != null) {
                ItemStack copy = armor.clone();
                ItemMeta meta = copy.getItemMeta();
                meta.setDisplayName(null);
                copy.setItemMeta(meta);
                player.getInventory().addItem(copy).values().forEach(overflow -> player.getWorld().dropItemNaturally(player.getLocation(), overflow));
            }
        } else if (slot >= 31 && slot <= 33) {
            int[] rgb = colors.get(player.getUniqueId());
            int i = slot - 31;
            rgb[i] = shift ? ThreadLocalRandom.current().nextInt(256)
                    : Math.max(0, Math.min(255, rgb[i] + (left ? 8 : -8)));
            Bukkit.getScheduler().runTask(plugin, () -> showArmor(player));
        }
    }

    private void clickBanner(Player player, int slot) {
        BannerSession session = banners.get(player.getUniqueId());
        if (session == null) return;
        if (slot == 0) {
            banners.put(player.getUniqueId(), new BannerSession());
        } else if (slot == 1) {
            if (session.nextColor != null) session.nextColor = null;
            else if (!session.patterns.isEmpty()) session.nextColor = session.patterns.removeLast().getColor();
            else session.base = null;
        } else if (slot == 8 || slot == 4) {
            player.getInventory().addItem(session.banner()).values().forEach(overflow -> player.getWorld().dropItemNaturally(player.getLocation(), overflow));
        } else if (slot == 7 && session.patterns.size() < 16) {
            if (session.base == null) session.base = randomColor();
            else if (session.nextColor == null) session.nextColor = randomColor();
            else {
                PatternType[] types = PatternType.values();
                PatternType pattern;
                do {
                    pattern = types[ThreadLocalRandom.current().nextInt(types.length)];
                } while (pattern == PatternType.BASE);
                session.patterns.add(new Pattern(session.nextColor, pattern));
                session.nextColor = null;
            }
        } else if (session.base == null || session.nextColor == null) {
            ItemStack selected = player.getOpenInventory().getTopInventory().getItem(slot);
            if (selected == null) return;
            String name = selected.getType().name();
            String suffix = session.base == null ? "_BANNER" : "_DYE";
            if (!name.endsWith(suffix)) return;
            DyeColor dye = DyeColor.valueOf(name.substring(0, name.length() - suffix.length()));
            if (session.base == null) session.base = dye;
            else session.nextColor = dye;
        } else {
            ItemStack selected = player.getOpenInventory().getTopInventory().getItem(slot);
            if (selected == null || !(selected.getItemMeta() instanceof BannerMeta meta) || meta.getPatterns().isEmpty()) return;
            session.patterns.add(meta.getPatterns().getLast());
            session.nextColor = null;
        }
        Bukkit.getScheduler().runTask(plugin, () -> showBanner(player));
    }

    private DyeColor randomColor() {
        DyeColor[] values = DyeColor.values();
        return values[ThreadLocalRandom.current().nextInt(values.length)];
    }

    private static final class BannerSession {
        DyeColor base;
        DyeColor nextColor;
        final List<Pattern> patterns = new ArrayList<>();

        ItemStack banner() {
            Material material = Material.matchMaterial((base == null ? DyeColor.WHITE : base).name() + "_BANNER");
            ItemStack stack = new ItemStack(material == null ? Material.WHITE_BANNER : material);
            BannerMeta meta = (BannerMeta) stack.getItemMeta();
            meta.setPatterns(patterns);
            stack.setItemMeta(meta);
            return stack;
        }
    }

    private static final class MenuHolder implements InventoryHolder {
        final String kind;
        Inventory inventory;
        MenuHolder(String kind) { this.kind = kind; }
        @Override public Inventory getInventory() { return inventory; }
    }
}
