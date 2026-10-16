package work.alsace.buildercore;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import work.alsace.buildercore.commands.home.SetHomeCommand;
import work.alsace.buildercore.commands.home.HomesCommand;
import work.alsace.buildercore.commands.players.SpeedCommand;
import work.alsace.buildercore.commands.time.TimeCommand;
import work.alsace.buildercore.commands.time.PTimeCommand;
import work.alsace.buildercore.commands.weather.PWeatherCommand;
import work.alsace.buildercore.commands.weather.WeatherCommand;
import work.alsace.buildercore.commands.tools.BuilderCoreCommand;
import work.alsace.buildercore.commands.warp.DelWarpCommand;
import work.alsace.buildercore.commands.warp.SetWarpCommand;
import work.alsace.buildercore.features.BuilderTools;
import work.alsace.buildercore.features.BuildingSettings;
import work.alsace.buildercore.listeners.ChestListener;
import work.alsace.buildercore.service.HomeDataLoader;
import work.alsace.buildercore.service.WarpDataLoader;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Logger;

/** 离线回归：实际执行命令、YAML 读写和库存事件；java -ea 运行。 */
public final class SelectedFixesCheck {
    private static <T> T proxy(Class<T> type, BiFunction<String, Object[], Object> calls) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (p, method, args) -> calls.apply(method.getName(), args)));
    }

    private static void field(Class<?> owner, Object target, String name, Object value) throws Exception {
        var field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    public static void main(String[] args) throws Exception {
        Path folder = Files.createTempDirectory("buildercore-check-");
        try {
            run(folder);
        } finally {
            try (var paths = Files.walk(folder)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        System.out.println("Selected fixes: names, quota refresh, warp deletion, speed, time, weather, metadata, preview and dragon egg passed");
    }

    private static void run(Path folder) throws Exception {
        UUID id = UUID.randomUUID();
        boolean[] loaded = {true};
        boolean[] permitted = {true};
        boolean[] flying = {false};
        long[] time = {0};
        int[] timeWrites = {0};
        int[] speedWrites = {0};
        float[] speed = {0};
        String[] quota = {"unlimited"};
        boolean[] otherWeather = {true};
        boolean[] storm = {false};
        boolean[] thunder = {false};
        WeatherType[] personalWeather = {null};
        long[] personalTime = {-1};
        boolean[] relativeTime = {true};
        int[] resetTime = {0};
        Server[] servers = {null};
        List<Component> messages = new ArrayList<>();
        World world = proxy(World.class, (name, values) -> switch (name) {
            case "getName" -> "world";
            case "getUID" -> id;
            case "getTime" -> time[0];
            case "setStorm" -> { storm[0] = (boolean) values[0]; yield null; }
            case "setThundering" -> { thunder[0] = (boolean) values[0]; yield null; }
            case "setTime" -> { time[0] = (long) values[0]; timeWrites[0]++; yield null; }
            default -> throw new AssertionError(name);
        });
        Player player = proxy(Player.class, (name, values) -> switch (name) {
            case "getUniqueId" -> id;
            case "getName" -> "Builder";
            case "getWorld" -> world;
            case "getServer" -> servers[0];
            case "setPlayerWeather" -> { personalWeather[0] = (WeatherType) values[0]; yield null; }
            case "resetPlayerWeather" -> { personalWeather[0] = null; yield null; }
            case "setPlayerTime" -> { personalTime[0] = (long) values[0]; relativeTime[0] = (boolean) values[1]; yield null; }
            case "resetPlayerTime" -> { resetTime[0]++; yield null; }
            case "getLocation" -> new Location(world, 12.5, 64, -3, 90, 15);
            case "hasPermission" -> permitted[0] && !values[0].toString().startsWith("buildercore.commands.sethome.other")
                    && (!values[0].equals("buildercore.commands.pweather.other") || otherWeather[0]);
            case "getEffectivePermissions" -> Set.of(new PermissionAttachmentInfo(proxy(org.bukkit.permissions.Permissible.class, (n, v) -> null),
                    "buildercore.home.max." + quota[0], null, true));
            case "isOnline" -> true;
            case "isFlying" -> flying[0];
            case "setWalkSpeed", "setFlySpeed" -> {
                speed[0] = (float) values[0]; speedWrites[0]++; yield null;
            }
            case "sendMessage" -> { if (values[0] instanceof Component c) messages.add(c); yield null; }
            default -> throw new AssertionError(name);
        });
        CommandSender console = proxy(CommandSender.class, (name, values) -> switch (name) {
            case "hasPermission" -> true;
            case "sendMessage" -> null;
            default -> throw new AssertionError(name);
        });
        BukkitScheduler scheduler = proxy(BukkitScheduler.class, (name, values) -> {
            if (!name.equals("runTaskAsynchronously")) throw new AssertionError(name);
            ((Runnable) values[1]).run();
            return proxy(BukkitTask.class, (n, v) -> null);
        });
        Server server = proxy(Server.class, (name, values) -> switch (name) {
            case "getPlayer" -> player;
            case "getOnlinePlayers" -> List.of(player);
            case "getWorld" -> loaded[0] ? world : null;
            case "getScheduler" -> scheduler;
            case "getLogger" -> Logger.getLogger("SelectedFixesCheck");
            default -> throw new AssertionError(name);
        });
        servers[0] = server;
        field(Bukkit.class, null, "server", server);
        // JavaPlugin 构造器要求服务端加载器，离线检查只注入实际用到的字段。
        var unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        var unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        BuilderCore plugin = (BuilderCore) unsafe.allocateInstance(BuilderCore.class);
        field(JavaPlugin.class, plugin, "server", server);
        field(JavaPlugin.class, plugin, "dataFolder", folder.toFile());
        field(JavaPlugin.class, plugin, "description", new org.bukkit.plugin.PluginDescriptionFile(new java.io.StringReader(
                "name: BuilderCore\nversion: test\nmain: work.alsace.buildercore.BuilderCore\nauthors: [Check]\n")));
        field(JavaPlugin.class, plugin, "logger", Logger.getLogger("SelectedFixesCheck"));
        YamlConfiguration config = new YamlConfiguration();
        field(JavaPlugin.class, plugin, "newConfig", config);
        field(BuilderCore.class, plugin, "homeProfiles", new HashMap<UUID, HomeDataLoader>());
        field(BuilderCore.class, plugin, "warpProfiles", new HashMap<String, WarpDataLoader>());
        BuilderCore.instance = plugin;

        // 旧版本标准 YAML 文件仍可读取，新增点号名称不会改变已有条目。
        Path users = Files.createDirectories(folder.resolve("userdata"));
        Files.writeString(users.resolve(id + ".yml"), "Home:\n  legacy:\n    World: world\n    X: 1\n    Y: 64\n    Z: 2\n    Yaw: 90\n    Pitch: 0\n");
        Files.writeString(folder.resolve("warps.yml"), "Warps:\n  legacy:\n    World: world\n    X: 1\n    Y: 64\n    Z: 2\n    Yaw: 90\n    Pitch: 0\n");
        HomeDataLoader homes = new HomeDataLoader(id);
        plugin.homeProfiles.put(id, homes);
        WarpDataLoader warps = new WarpDataLoader(plugin);
        plugin.warpProfiles.put("warps", warps);
        assert homes.getHome("legacy").getX() == 1;
        assert warps.getWarp("legacy").getX() == 1;
        String[] names = {"home1", "工地", "a.b", "a/b", "<red>", "o'neil", "a:1"};
        for (String name : names) {
            assert new SetHomeCommand().onCommand(player, null, "sethome", new String[]{name});
            assert new SetWarpCommand().onCommand(player, null, "setwarp", new String[]{name});
            assert homes.getHome(name) != null : name;
            assert warps.getWarp(name) != null : name;
        }
        assert new HomesCommand().onCommand(player, null, "homes", new String[0]);
        assert messages.getLast().toString().contains("<red>"); // 名称保留为普通文本
        homes = new HomeDataLoader(id);
        warps = new WarpDataLoader(plugin);
        for (String name : names) {
            assert homes.getHome(name).getX() == 12.5 : name;
            assert warps.getWarp(name).getX() == 12.5 : name;
        }
        assert homes.getHome("legacy").getX() == 1;
        assert warps.getWarp("legacy").getX() == 1;
        homes.delHome("a.b");
        assert new HomeDataLoader(id).getHome("a.b") == null;
        assert new HomeDataLoader(id).getHome("a/b") != null;
        // 世界未加载也可删除正式名称，删除后重载仍不存在。
        loaded[0] = false;
        warps = new WarpDataLoader(plugin);
        plugin.warpProfiles.put("warps", warps);
        assert warps.getWarp("a.b").getWorld() == null;
        DelWarpCommand delete = new DelWarpCommand();
        assert !delete.onCommand(player, null, "delwarp", new String[]{"missing"});
        assert delete.onTabComplete(player, null, "delwarp", new String[]{"a."}).contains("a.b");
        assert delete.onTabComplete(console, null, "delwarp", new String[]{""}).isEmpty();
        assert delete.onCommand(player, null, "delwarp", new String[]{"a.b"});
        assert new WarpDataLoader(plugin).getWarp("a.b") == null;
        assert new WarpDataLoader(plugin).getWarp("a/b") != null;
        loaded[0] = true;
        homes = new HomeDataLoader(id);
        plugin.homeProfiles.put(id, homes);
        quota[0] = "0";
        assert new SetHomeCommand().onCommand(player, null, "sethome", new String[]{"no-quota"});
        assert homes.getHome("no-quota") == null;
        quota[0] = Integer.toString(homes.getHomes().size() + 1);
        assert new SetHomeCommand().onCommand(player, null, "sethome", new String[]{"new-quota"});
        assert homes.getHome("new-quota") != null; // 无需重新登录或重载
        quota[0] = "0";
        assert new SetHomeCommand().onCommand(player, null, "sethome", new String[]{"revoked-quota"});
        assert homes.getHome("revoked-quota") == null;

        SpeedCommand speedCommand = new SpeedCommand();
        for (boolean fly : new boolean[]{false, true}) {
            flying[0] = fly;
            for (String value : new String[]{"NaN", "Infinity", "-Infinity", "1e100", "0", "0.09", "10.1", "15"}) {
                for (String[] input : List.of(new String[]{value}, new String[]{"walk", value},
                        new String[]{"fly", value}, new String[]{"walk", value, "Builder"},
                        new String[]{"fly", value, "Builder"})) {
                    int before = speedWrites[0];
                    assert !speedCommand.onCommand(player, null, "speed", input) : Arrays.toString(input);
                    assert speedWrites[0] == before;
                }
            }
            for (String value : new String[]{"0.1", "10"}) {
                for (String[] input : List.of(new String[]{value}, new String[]{"walk", value},
                        new String[]{"fly", value}, new String[]{"walk", value, "Builder"},
                        new String[]{"fly", value, "Builder"})) {
                    assert speedCommand.onCommand(player, null, "speed", input);
                    assert speed[0] == Float.parseFloat(value) / 10F;
                }
            }
        }

        TimeCommand timeCommand = new TimeCommand();
        for (String[] input : List.of(new String[]{"day"}, new String[]{"set", "1000"},
                new String[]{"add", "1000"}, new String[]{"day", "world"})) {
            int before = timeWrites[0];
            assert !timeCommand.onCommand(console, null, "time", input);
            assert timeWrites[0] == before;
        }
        assert timeCommand.onTabComplete(console, null, "time", new String[]{""}).isEmpty();
        assert timeCommand.onCommand(player, null, "time", new String[]{"day"});
        assert time[0] == 0;
        assert timeCommand.onCommand(player, null, "time", new String[]{"set", "1000"});
        assert time[0] == 1000;
        assert timeCommand.onCommand(player, null, "time", new String[]{"add", "1000"});
        assert time[0] == 2000;
        for (String[] input : List.of(new String[]{"day", "world"}, new String[]{"set", "day", "world"},
                new String[]{"add", "99999999999999999999999999"})) {
            int before = timeWrites[0];
            assert !timeCommand.onCommand(player, null, "time", input);
            assert timeWrites[0] == before;
        }
        assert timeCommand.onTabComplete(player, null, "time", new String[]{"day", ""}).isEmpty();
        permitted[0] = false;
        assert !timeCommand.onCommand(player, null, "time", new String[]{"day"});
        permitted[0] = true;

        PTimeCommand ptime = new PTimeCommand();
        String[] keywords = {"day", "morning", "noon", "afternoon", "sunset", "night", "midnight", "sunrise"};
        long[] expected = {0, 3000, 6000, 9000, 12000, 15000, 18000, 21000};
        for (int i = 0; i < keywords.length; i++) {
            assert timeCommand.onCommand(player, null, "time", new String[]{keywords[i]});
            assert ptime.onCommand(player, null, "ptime", new String[]{keywords[i]});
            assert time[0] == expected[i] && personalTime[0] == expected[i];
            assert !relativeTime[0]; // false 使用固定个人时刻
        }
        assert ptime.onCommand(player, null, "ptime", new String[]{"5000", "Builder"});
        assert personalTime[0] == 5000 && !relativeTime[0];
        assert ptime.onCommand(player, null, "ptime", new String[]{"reset"});
        assert resetTime[0] == 1;

        PWeatherCommand pweather = new PWeatherCommand();
        assert pweather.onCommand(player, null, "pweather", new String[]{"clear"});
        assert personalWeather[0] == WeatherType.CLEAR;
        assert pweather.onCommand(player, null, "pweather", new String[]{"rain"});
        assert personalWeather[0] == WeatherType.DOWNFALL;
        assert pweather.onCommand(player, null, "pweather", new String[]{"reset"});
        assert personalWeather[0] == null;
        otherWeather[0] = false;
        assert !pweather.onCommand(player, null, "pweather", new String[]{"rain", "Builder"});
        assert personalWeather[0] == null;
        assert pweather.onTabComplete(player, null, "pweather", new String[]{"rain", ""}).isEmpty();
        otherWeather[0] = true;
        assert pweather.onCommand(player, null, "pweather", new String[]{"rain", "Builder"});
        assert personalWeather[0] == WeatherType.DOWNFALL;
        assert pweather.onTabComplete(player, null, "pweather", new String[]{"rain", ""}).equals(List.of("Builder"));
        assert pweather.onTabComplete(player, null, "pweather", new String[]{""}).contains("reset");
        WeatherCommand weather = new WeatherCommand();
        assert weather.onCommand(player, null, "weather", new String[]{"thunder"});
        assert storm[0] && thunder[0];
        assert weather.onCommand(player, null, "weather", new String[]{"rain"});
        assert storm[0] && !thunder[0];
        assert weather.onCommand(player, null, "weather", new String[]{"clear"});
        assert !storm[0] && !thunder[0];
        assert new BuilderCoreCommand(plugin).onCommand(player, null, "buildercore", new String[]{"info"});
        String info = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(messages.getLast());
        assert info.contains("BuilderCore") && !info.contains("AlsaceCore");

        Class<?> holderType = Class.forName("work.alsace.buildercore.listeners.ChestListener$PreviewHolder");
        var constructor = holderType.getDeclaredConstructor();
        constructor.setAccessible(true);
        InventoryHolder holder = (InventoryHolder) constructor.newInstance();
        Inventory top = proxy(Inventory.class, (name, values) -> switch (name) {
            case "getHolder" -> holder;
            case "getSize" -> 27;
            default -> throw new AssertionError(name);
        });
        InventoryView view = proxy(InventoryView.class, (name, values) -> switch (name) {
            case "getTopInventory", "getBottomInventory", "getInventory" -> top;
            case "getPlayer" -> player;
            case "getOnlinePlayers" -> List.of(player);
            case "convertSlot" -> values[0];
            default -> throw new AssertionError(name);
        });
        ChestListener chest = new ChestListener();
        for (ClickType click : ClickType.values()) {
            for (int slot : new int[]{0, 30, -999}) {
                InventoryClickEvent event = new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER,
                        slot, click, InventoryAction.UNKNOWN, 0);
                chest.onInventoryClick(event);
                assert event.isCancelled() : click;
            }
        }
        ItemStack stone = new ItemStack() {
            @Override
            public Material getType() { return Material.STONE; }
        };
        for (int slot : new int[]{0, 30}) {
            InventoryDragEvent event = new InventoryDragEvent(view, stone, stone, false, Map.of(slot, stone));
            chest.onInventoryDrag(event);
            assert event.isCancelled();
        }
        Inventory normal = proxy(Inventory.class, (name, values) -> null);
        InventoryView ordinary = proxy(InventoryView.class, (name, values) ->
                name.equals("convertSlot") ? values[0] : name.equals("getPlayer") ? player : normal);
        InventoryClickEvent ordinaryClick = new InventoryClickEvent(ordinary, InventoryType.SlotType.CONTAINER,
                0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        chest.onInventoryClick(ordinaryClick);
        assert !ordinaryClick.isCancelled(); // 普通箱子和末影箱不受限制
        InventoryDragEvent ordinaryDrag = new InventoryDragEvent(ordinary, stone, stone, false, Map.of(0, stone));
        chest.onInventoryDrag(ordinaryDrag);
        assert !ordinaryDrag.isCancelled();

        BuildingSettings settings = new BuildingSettings(plugin, new BuilderTools(plugin));
        Block egg = proxy(Block.class, (name, values) -> Material.DRAGON_EGG);
        for (boolean disabled : new boolean[]{false, true}) {
            config.set("building-settings.disable-dragon-egg-teleport", disabled);
            BlockFromToEvent event = new BlockFromToEvent(egg, BlockFace.EAST);
            settings.onDragonEgg(event);
            assert event.isCancelled() == disabled;
        }
    }
}
