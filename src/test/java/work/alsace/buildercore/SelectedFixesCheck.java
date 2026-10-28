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

    private static final class AgreementDatabase extends work.alsace.buildercore.utils.DataBaseManager {
        java.util.concurrent.CompletableFuture<Boolean> read;
        java.util.concurrent.CompletableFuture<Void> write;
        private AgreementDatabase() { super(null, "", "", "", ""); }
        @Override public java.util.concurrent.CompletableFuture<Boolean> hasPlayerAgreed(UUID id) { return read; }
        @Override public java.util.concurrent.CompletableFuture<Void> setPlayerAgreed(UUID id, boolean agreed) { return write; }
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
        System.out.println("Selected fixes: command help/completion, visibility, quotas, YAML/aliases, back history, finite inputs, agreement persistence, restrictions, serialization, speed/time/weather and previews passed");
    }

    private static void run(Path folder) throws Exception {
        UUID id = UUID.randomUUID();
        boolean[] loaded = {true};
        boolean[] quotaGranted = {true};
        boolean[] visible = {true};
        java.util.concurrent.CompletableFuture<Boolean>[] teleport = new java.util.concurrent.CompletableFuture[]{null};
        Location[] teleported = {null};
        List<Runnable> timers = new ArrayList<>();
        Map<String, org.bukkit.command.PluginCommand> registered = new HashMap<>();
        boolean[] permitted = {true};
        boolean[] flying = {false};
        boolean[] syncTeleport = {false};
        long[] time = {0};
        int[] timeWrites = {0};
        int[] speedWrites = {0};
        float[] speed = {0};
        String[] quota = {"unlimited"};
        boolean[] otherWeather = {true};
        boolean[] otherTp = {true};
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
                    && (!values[0].equals("buildercore.commands.pweather.other") || otherWeather[0])
                    && (!values[0].equals("buildercore.commands.tp.other") || otherTp[0]);
            case "getEffectivePermissions" -> Set.of(new PermissionAttachmentInfo(proxy(org.bukkit.permissions.Permissible.class, (n, v) -> null),
                    "buildercore.home.max." + quota[0], null, quotaGranted[0]));
            case "isOnline" -> true;
            case "hashCode" -> id.hashCode();
            case "equals" -> values[0] instanceof Player p && p.getUniqueId().equals(id);
            case "hasPlayedBefore" -> true;
            case "getShoulderEntityLeft", "getShoulderEntityRight" -> null;
            case "canSee" -> visible[0];
            case "teleport" -> syncTeleport[0];
            case "teleportAsync" -> { teleported[0] = (Location) values[0]; yield teleport[0]; }
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
            if (name.equals("runTaskTimer")) timers.add((Runnable) values[1]);
            else if (name.equals("runTaskAsynchronously") || name.equals("runTask")) ((Runnable) values[1]).run();
            else throw new AssertionError(name);
            return proxy(BukkitTask.class, (n, v) -> n.equals("getTaskId") ? 1 : null);
        });
        int[] registrations = {0};
        org.bukkit.plugin.PluginManager manager = proxy(org.bukkit.plugin.PluginManager.class, (name, values) -> {
            if (name.equals("registerEvents")) registrations[0]++;
            return name.equals("isPluginEnabled") ? false : null;
        });
        org.bukkit.command.CommandMap commandMap = proxy(org.bukkit.command.CommandMap.class, (name, values) -> null);
        Server server = proxy(Server.class, (name, values) -> switch (name) {
            case "getPlayer" -> player;
            case "getPlayerExact", "getOfflinePlayerIfCached" -> values[0].equals("Builder") ? player : null;
            case "getPluginCommand" -> registered.get(values[0].toString().replace("buildercore:", ""));
            case "getPluginManager" -> manager;
            case "getCommandMap" -> commandMap;
            case "getOnlinePlayers" -> List.of(player);
            case "isPrimaryThread" -> true;
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
        field(JavaPlugin.class, plugin, "description", new org.bukkit.plugin.PluginDescriptionFile(new java.io.FileReader("src/main/resources/plugin.yml")));
        field(JavaPlugin.class, plugin, "logger", Logger.getLogger("SelectedFixesCheck"));
        YamlConfiguration config = new YamlConfiguration();
        field(JavaPlugin.class, plugin, "newConfig", config);
        field(BuilderCore.class, plugin, "homeProfiles", new HashMap<UUID, HomeDataLoader>());
        field(BuilderCore.class, plugin, "warpProfiles", new HashMap<String, WarpDataLoader>());
        BuilderCore.instance = plugin;
        field(BuilderCore.class, plugin, "hasAgree", new java.util.concurrent.ConcurrentHashMap<String, Boolean>());
        field(BuilderCore.class, plugin, "hasIgnored", new HashMap<String, Boolean>());
        var commandConstructor = org.bukkit.command.PluginCommand.class.getDeclaredConstructor(String.class, org.bukkit.plugin.Plugin.class);
        commandConstructor.setAccessible(true);
        for (var entry : plugin.getDescription().getCommands().entrySet()) {
            var command = commandConstructor.newInstance(entry.getKey(), plugin);
            command.setUsage((String) entry.getValue().get("usage"));
            command.setDescription((String) entry.getValue().get("description"));
            command.setPermission((String) entry.getValue().get("permission"));
            registered.put(entry.getKey(), command);
        }

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

        quota[0] = "unlimited";
        quotaGranted[0] = false;
        homes.calculateMaxHomes();
        assert homes.getMaxHomes() == 0 : "denied quota must not grant unlimited homes";
        quotaGranted[0] = true;
        homes.calculateMaxHomes();
        assert homes.getMaxHomes() == Integer.MAX_VALUE;

        warps.addWarp("alias-target", "alias.name", new Location(world, 8, 64, 9, 17.5F, 6.25F));
        warps = new WarpDataLoader(plugin);
        assert warps.getWarp("alias.name").getYaw() == 17.5F;
        assert warps.getWarpAlias("alias-target").equals("alias.name");
        plugin.warpProfiles.put("warps", warps);
        assert !new SetWarpCommand().onCommand(player, registered.get("setwarp"), "setwarp", new String[]{"different", "alias.name"});
        assert warps.getWarp("different") == null;
        warps.delWarp("alias.name");
        assert new WarpDataLoader(plugin).getWarp("alias-target") == null;
        assert new WarpDataLoader(plugin).getRealWarpName("alias.name").equals("alias.name");
        loaded[0] = false;
        homes = new HomeDataLoader(id);
        assert homes.getHome("legacy").getWorld() == null;
        loaded[0] = true;
        assert homes.getHome("legacy").getWorld() == world : "world may load after player data";
        Location copy = homes.getHome("legacy");
        copy.setX(999);
        assert homes.getHome("legacy").getX() == 999 : "Location API remains mutable";
        World otherWorld = proxy(World.class, (name, values) -> name.equals("getName") ? "other" : null);
        copy.setWorld(otherWorld);
        assert homes.getHome("legacy").getWorld() == otherWorld : "getHome must preserve API changes to an already resolved world";
        copy.setWorld(world);
        copy.setX(1);
        homes.addHome("immediate", new Location(world, 2, 3, 4));
        homes.delHome("immediate");
        assert new HomeDataLoader(id).getHome("immediate") == null;
        Path corrupt = users.resolve(UUID.randomUUID() + ".yml");
        Files.writeString(corrupt, "Home: [broken");
        String invalidYaml = Files.readString(corrupt);
        boolean failed = false;
        try { new HomeDataLoader(UUID.fromString(corrupt.getFileName().toString().replace(".yml", ""))); }
        catch (IllegalStateException expectedError) { failed = true; }
        assert failed && Files.readString(corrupt).equals(invalidYaml);
        plugin.homeProfiles.put(id, homes);
        plugin.warpProfiles.put("warps", new WarpDataLoader(plugin));

        var registerCompletions = BuilderCore.class.getDeclaredMethod("registerCompletions");
        registerCompletions.setAccessible(true);
        registered.get("weather").setExecutor(weather);
        registered.get("gamemode").setExecutor(new work.alsace.buildercore.commands.players.GameModeCommand());
        registerCompletions.invoke(plugin);
        for (var command : registered.values()) {
            assert command.getTabCompleter() != null : command.getName();
            assert !command.getUsage().isBlank() : command.getName();
            if (work.alsace.buildercore.commands.CommandHelp.supportsFlag(command.getName()))
                assert command.tabComplete(player, command.getName(), new String[]{"--h"}).equals(List.of("--help"));
        }
        assert registered.get("weather").tabComplete(player, "weather", new String[]{"r"}).equals(List.of("rain"));
        assert registered.get("home").tabComplete(player, "home", new String[]{"LEG"}).equals(List.of("legacy"));
        assert registered.get("gamemode").tabComplete(player, "gm", new String[]{"c"}).equals(List.of("creative"));
        assert registered.get("fly").tabComplete(player, "fly", new String[]{"B"}).equals(List.of("Builder"));
        visible[0] = false;
        assert registered.get("fly").tabComplete(player, "fly", new String[]{"B"}).isEmpty();
        visible[0] = true;
        permitted[0] = false;
        assert registered.get("fly").tabComplete(player, "fly", new String[]{"B"}).isEmpty();
        permitted[0] = true;
        assert registered.get("warp").tabComplete(console, "warp", new String[]{"leg"}).isEmpty();
        otherTp[0] = false;
        assert registered.get("tp").tabComplete(player, "tp", new String[]{"Other", ""}).isEmpty();
        assert registered.get("tp").tabComplete(player, "tp", new String[]{"-1e2", ""}).equals(List.of("~"));
        otherTp[0] = true;
        work.alsace.buildercore.commands.CommandHelp.show(player, registered.get("tp"), "tp");
        assert messages.getLast().toString().contains("<玩家A>");
        assert new BuilderCoreCommand(plugin).onCommand(player, registered.get("buildercore"), "buildercore", new String[]{"help", "tp"});

        int initialRegistrations = registrations[0];
        new work.alsace.buildercore.listeners.AFKListener(plugin);
        assert registrations[0] == initialRegistrations + 1 : "AFK constructor retains event registration";
        work.alsace.buildercore.listeners.AFKListener.class.getMethod("beforePlayerJoin", org.bukkit.event.player.AsyncPlayerPreLoginEvent.class);
        work.alsace.buildercore.listeners.Misc.class.getMethod("onBlockFadeEvent", org.bukkit.event.block.BlockFadeEvent.class);
        for (String literal : List.of("home", "sethome", "warp", "setwarp", "itemname", "head", "/pa", "/s"))
            assert !work.alsace.buildercore.commands.CommandHelp.requested(player, registered.get(literal), literal, new String[]{"--help"}) : literal;
        new SetHomeCommand().onCommand(player, registered.get("sethome"), "sethome", new String[]{"--help"});
        assert plugin.homeProfiles.get(id).getHome("--help") != null;
        new SetWarpCommand().onCommand(player, registered.get("setwarp"), "setwarp", new String[]{"--help"});
        assert plugin.warpProfiles.get("warps").getWarp("--help") != null;
        var apiWarps = plugin.warpProfiles.get("warps");
        apiWarps.addWarp("api-upsert", "api-alias", new Location(world, 1, 2, 3));
        apiWarps.addWarp("api-upsert", "api-alias", new Location(world, 4, 5, 6));
        assert apiWarps.getWarp("api-alias").getX() == 4;
        Location mutableWarp = apiWarps.getWarp("api-upsert");
        mutableWarp.setWorld(otherWorld);
        assert apiWarps.getWarp("api-upsert").getWorld() == otherWorld : "getWarp must preserve API world mutations";
        mutableWarp.setWorld(world);
        Set<String> warpSnapshot = apiWarps.getWarps();
        warpSnapshot.remove("api-upsert");
        assert apiWarps.getWarp("api-upsert") != null : "warp names are a mutable snapshot";
        Set<String> homeView = plugin.homeProfiles.get(id).getHomes();
        plugin.homeProfiles.get(id).addHome("api-live-view", new Location(world, 0, 0, 0));
        assert homeView.contains("api-live-view") : "home names remain a live view";
        plugin.homeProfiles.get(id).delHome("api-live-view");
        Path mixedFile = users.resolve(UUID.randomUUID() + ".yml");
        Files.writeString(mixedFile, "Home:\n  MiXeD:\n    World: world\n    X: 17\n    Y: 64\n    Z: 1\n");
        var mixed = new HomeDataLoader(UUID.fromString(mixedFile.getFileName().toString().replace(".yml", "")));
        assert mixed.getHome("mixed").getWorld() == world && mixed.getHome("MIXED").getX() == 17;
        mixed.delHome("mixed");
        assert new HomeDataLoader(UUID.fromString(mixedFile.getFileName().toString().replace(".yml", ""))).getHomes().isEmpty();
        HomeDataLoader offlineHomes = plugin.homeProfiles.remove(id);
        assert registered.get("home").tabComplete(player, "home", new String[]{"Builder:leg"}).equals(List.of("Builder:legacy"));
        assert !plugin.homeProfiles.containsKey(id) : "completion must not cache or create a new profile";
        Path offlineData = users.resolve(id + ".yml");
        String offlineYaml = Files.readString(offlineData);
        try {
            Files.writeString(offlineData, "Home: [broken");
            assert registered.get("home").tabComplete(player, "home", new String[]{"Builder:leg"}).isEmpty();
            assert Files.readString(offlineData).equals("Home: [broken");
        } finally { Files.writeString(offlineData, offlineYaml); }
        plugin.homeProfiles.put(id, offlineHomes);

        var back = new work.alsace.buildercore.commands.teleport.BackCommand(plugin);
        plugin.backHistory = 5;
        back.addToHistory(player);
        teleport[0] = new java.util.concurrent.CompletableFuture<>();
        assert back.onCommand(player, null, "back", new String[0]);
        Location firstReturn = teleported[0];
        teleport[0].complete(false);
        teleport[0] = new java.util.concurrent.CompletableFuture<>();
        assert back.onCommand(player, null, "back", new String[0]);
        assert teleported[0].equals(firstReturn) : "failed teleport must retain the history";
        back.onTeleport(new org.bukkit.event.player.PlayerTeleportEvent(player, player.getLocation(), firstReturn));
        teleport[0].complete(true);
        teleported[0] = null;
        assert back.onCommand(player, null, "back", new String[0]);
        assert teleported[0] == null : "returning must not reinsert its own history";

        back.addToHistory(player);
        teleport[0] = new java.util.concurrent.CompletableFuture<>();
        back.onCommand(player, null, "back", new String[0]);
        Location backTarget = teleported[0];
        back.onTeleport(new org.bukkit.event.player.PlayerTeleportEvent(player, player.getLocation(), new Location(world, 99, 65, 99)));
        var historyField = work.alsace.buildercore.commands.teleport.BackCommand.class.getDeclaredField("locationHistory");
        historyField.setAccessible(true);
        var histories = (Map<UUID, Deque<Location>>) historyField.get(back);
        Location laterRecord = histories.get(id).getLast();
        back.onTeleport(new org.bukkit.event.player.PlayerTeleportEvent(player, player.getLocation(), backTarget));
        teleport[0].complete(true);
        assert histories.get(id).size() == 1 && histories.get(id).getLast() == laterRecord
                : "returning must preserve unrelated teleports and remove only its original records";

        var tpa = new work.alsace.buildercore.service.TPAHandler(plugin);
        tpa.sendTPARequest(player, player, false);
        var requestsField = work.alsace.buildercore.service.TPAHandler.class.getDeclaredField("tpaRequests");
        requestsField.setAccessible(true);
        Map<UUID, Object> requests = (Map<UUID, Object>) requestsField.get(tpa);
        assert requests.size() == 1;
        tpa.onQuit(new org.bukkit.event.player.PlayerQuitEvent(player, Component.empty()));
        assert requests.isEmpty();
        Class<?> requestClass = Class.forName("work.alsace.buildercore.service.TPAHandler$TPARequest");
        var expiredRequest = requestClass.getDeclaredConstructor(UUID.class, boolean.class, long.class);
        expiredRequest.setAccessible(true);
        requests.put(id, expiredRequest.newInstance(id, false, System.currentTimeMillis() - 61_000));
        timers.getLast().run();
        assert requests.isEmpty();

        var vote = new work.alsace.buildercore.commands.tools.RestartVoteCommand(plugin);
        vote.RunVote("offline regression");
        var votesField = work.alsace.buildercore.commands.tools.RestartVoteCommand.class.getDeclaredField("yesVote");
        votesField.setAccessible(true);
        vote.onCommand(player, null, "restart-vote", new String[]{"#yes"});
        vote.onCommand(player, null, "restart-vote", new String[]{"#yes"});
        assert votesField.getInt(vote) == 1 : "each eligible player can vote only once";
        permitted[0] = false;
        assert !vote.onCommand(player, null, "restart-vote", new String[]{"#yes"});
        permitted[0] = true;
        assert new work.alsace.buildercore.commands.players.DismountParrotCommand().onCommand(player, null, "dismount", new String[0]);

        var plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText();
        for (boolean result : new boolean[]{false, true}) {
            teleport[0] = java.util.concurrent.CompletableFuture.completedFuture(result);
            syncTeleport[0] = result;
            new work.alsace.buildercore.commands.home.HomeCommand().onCommand(player, null, "home", new String[]{"legacy"});
            assert plain.serialize(messages.getLast()).contains(result ? "已传送" : "失败");
            assert new work.alsace.buildercore.commands.warp.WarpCommand().onCommand(player, null, "warp", new String[]{"legacy"}) == result;
            assert plain.serialize(messages.getLast()).contains(result ? "已传送" : "失败");
            new work.alsace.buildercore.commands.teleport.TPCommand(plugin).onCommand(player, null, "tp", new String[]{"1", "2", "3"});
            assert plain.serialize(messages.getLast()).contains(result ? "已将" : "失败");
        }

        var parse = work.alsace.buildercore.commands.teleport.TPCommand.class.getDeclaredMethod("parseLocation", CommandSender.class, Player.class, String.class, String.class, String.class);
        parse.setAccessible(true);
        var tp = new work.alsace.buildercore.commands.teleport.TPCommand(plugin);
        for (String value : List.of("NaN", "Infinity", "~Infinity", "1e1000"))
            assert ((Optional<?>) parse.invoke(tp, player, player, value, "0", "0")).isEmpty();
        assert ((Optional<Location>) parse.invoke(tp, player, player, "~1", "64", "~")).orElseThrow().getX() == 13.5;
        var serializer = new work.alsace.buildercore.utils.SerializeInventory();
        ItemStack[] emptyInventory = new ItemStack[]{null, null};
        assert serializer.deserializeInventory(serializer.serializeInventory(emptyInventory)).length == 2;
        for (String bad : List.of("not-base64!", "AAAA")) {
            boolean rejected = false;
            try { serializer.deserializeInventory(bad); } catch (java.io.IOException expectedError) { rejected = true; }
            assert rejected : "corruption must not return an empty inventory";
        }

        for (String value : List.of("NaN", "Infinity", "-Infinity")) {
            assert !new work.alsace.buildercore.commands.players.PlayerScaleCommand().onCommand(player, null, "scale", new String[]{value});
            assert !new work.alsace.buildercore.commands.players.ReachCommand().onCommand(player, null, "reach", new String[]{value});
        }
        var restrictions = new work.alsace.buildercore.listeners.Misc();
        permitted[0] = false;
        for (String name : List.of("/help 1", "/bukkit:plugins", "/minecraft:help BuilderCore", "/PLUGINS")) {
            var event = new org.bukkit.event.player.PlayerCommandPreprocessEvent(player, name);
            restrictions.onPlayerCommandPreprocess(event);
            assert event.isCancelled() : name;
        }
        permitted[0] = true;
        var unrestricted = new org.bukkit.event.player.PlayerCommandPreprocessEvent(player, "/bukkit:plugins");
        restrictions.onPlayerCommandPreprocess(unrestricted);
        assert !unrestricted.isCancelled();
        var database = (AgreementDatabase) unsafe.allocateInstance(AgreementDatabase.class);
        database.read = new java.util.concurrent.CompletableFuture<>();
        database.write = new java.util.concurrent.CompletableFuture<>();
        field(BuilderCore.class, plugin, "databaseManager", database);
        plugin.agreement = true;
        plugin.url = "https://example.invalid/agreement";
        field(BuilderCore.class, plugin, "urlTitle", "Test agreement");
        var listener = new work.alsace.buildercore.listeners.PlayerListener(plugin);
        listener.onPlayerJoin(new org.bukkit.event.player.PlayerJoinEvent(player, Component.empty()));
        assert plugin.hasAgree.get("Builder") : "agreement must restrict players before the DB query completes";
        var movement = new org.bukkit.event.player.PlayerMoveEvent(player, new Location(world, 0, 0, 0), new Location(world, 1, 0, 0));
        listener.onPlayerMove(movement);
        assert movement.isCancelled();
        for (String name : List.of("/agreeExtra", "/disagreeExtra", "/tp Builder")) {
            var event = new org.bukkit.event.player.PlayerCommandPreprocessEvent(player, name);
            listener.onPlayerCommand(event);
            assert event.isCancelled() : name;
        }
        var agreeEvent = new org.bukkit.event.player.PlayerCommandPreprocessEvent(player, "/buildercore:agree");
        listener.onPlayerCommand(agreeEvent);
        assert !agreeEvent.isCancelled();
        var agree = new work.alsace.buildercore.commands.agree.AgreeCommand(plugin);
        agree.onCommand(player, null, "agree", new String[0]);
        assert plugin.hasAgree.get("Builder") : "must wait for successful persistence";
        database.write.completeExceptionally(new java.sql.SQLException("test failure"));
        assert plugin.hasAgree.get("Builder");
        database.write = new java.util.concurrent.CompletableFuture<>();
        agree.onCommand(player, null, "agree", new String[0]);
        database.write.complete(null);
        assert !plugin.hasAgree.get("Builder");
        database.read.complete(false);
        assert !plugin.hasAgree.get("Builder") : "late login query must not revoke a successful agreement";
        Path ownData = users.resolve(id + ".yml");
        String savedData = Files.readString(ownData);
        database.read = new java.util.concurrent.CompletableFuture<>();
        var logger = plugin.getLogger();
        var previousLevel = logger.getLevel();
        try {
            Files.writeString(ownData, "Home: [broken");
            logger.setLevel(java.util.logging.Level.OFF);
            listener.onPlayerJoin(new org.bukkit.event.player.PlayerJoinEvent(player, Component.empty()));
            assert plugin.hasAgree.get("Builder") && !plugin.homeProfiles.containsKey(id)
                    : "corrupt home data must not bypass agreement restrictions";
            assert Files.readString(ownData).equals("Home: [broken");
        } finally {
            logger.setLevel(previousLevel);
            Files.writeString(ownData, savedData);
        }
        database.read.completeExceptionally(new java.sql.SQLException("test query failure"));
        assert plugin.hasAgree.get("Builder");
        database.read = new java.util.concurrent.CompletableFuture<>();
        listener.onPlayerJoin(new org.bukkit.event.player.PlayerJoinEvent(player, Component.empty()));
        database.read.complete(true);
        assert !plugin.hasAgree.get("Builder");
        plugin.agreement = false;
        var realDatabase = (work.alsace.buildercore.utils.DataBaseManager) unsafe.allocateInstance(work.alsace.buildercore.utils.DataBaseManager.class);
        field(work.alsace.buildercore.utils.DataBaseManager.class, realDatabase, "ready", java.util.concurrent.CompletableFuture.failedFuture(new java.sql.SQLException("schema unavailable")));
        assert realDatabase.hasPlayerAgreed(id).isCompletedExceptionally();
        assert realDatabase.setPlayerAgreed(id, true).isCompletedExceptionally();
        assert realDatabase.delPlayerAgreed(id).isCompletedExceptionally();

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
            case "getPlayerExact", "getOfflinePlayerIfCached" -> values[0].equals("Builder") ? player : null;
            case "getPluginCommand" -> registered.get(values[0].toString().replace("buildercore:", ""));
            case "getPluginManager" -> manager;
            case "getCommandMap" -> commandMap;
            case "getOnlinePlayers" -> List.of(player);
            case "isPrimaryThread" -> true;
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

        Class<?> menuType = Class.forName("work.alsace.buildercore.features.BuilderTools$MenuHolder");
        var menuConstructor = menuType.getDeclaredConstructor(String.class);
        menuConstructor.setAccessible(true);
        InventoryHolder specialMenu = (InventoryHolder) menuConstructor.newInstance("special");
        Inventory specialInventory = proxy(Inventory.class, (name, values) -> switch (name) {
            case "getHolder" -> specialMenu;
            case "getSize" -> 9;
            default -> throw new AssertionError(name);
        });
        InventoryView menuView = proxy(InventoryView.class, (name, values) -> switch (name) {
            case "getTopInventory", "getBottomInventory", "getInventory" -> specialInventory;
            case "getPlayer" -> player;
            case "getItem" -> stone;
            case "convertSlot" -> values[0];
            default -> throw new AssertionError(name);
        });
        var menuClick = new InventoryClickEvent(menuView, InventoryType.SlotType.CONTAINER, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        permitted[0] = false;
        new BuilderTools(plugin).onClick(menuClick);
        assert menuClick.isCancelled() : "revoked menu permission must not access the player's inventory";
        permitted[0] = true;

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
