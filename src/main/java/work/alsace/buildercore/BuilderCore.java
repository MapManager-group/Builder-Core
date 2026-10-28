package work.alsace.buildercore;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import work.alsace.buildercore.commands.agree.AgreeCommand;
import work.alsace.buildercore.commands.agree.DisagreeCommand;
import work.alsace.buildercore.commands.alias.*;
import work.alsace.buildercore.commands.builderTools.*;
import work.alsace.buildercore.commands.home.DelHomeCommand;
import work.alsace.buildercore.commands.home.HomeCommand;
import work.alsace.buildercore.commands.home.HomesCommand;
import work.alsace.buildercore.commands.home.SetHomeCommand;
import work.alsace.buildercore.commands.itemEdit.ItemColorCommand;
import work.alsace.buildercore.commands.itemEdit.ItemNameCommand;
import work.alsace.buildercore.commands.itemEdit.ItemNbtCommand;
import work.alsace.buildercore.commands.players.*;
import work.alsace.buildercore.commands.teleport.*;
import work.alsace.buildercore.commands.time.PTimeCommand;
import work.alsace.buildercore.commands.time.TimeCommand;
import work.alsace.buildercore.commands.tools.*;
import work.alsace.buildercore.commands.warp.DelWarpCommand;
import work.alsace.buildercore.commands.warp.SetWarpCommand;
import work.alsace.buildercore.commands.warp.WarpCommand;
import work.alsace.buildercore.commands.warp.WarpsCommand;
import work.alsace.buildercore.commands.weather.PWeatherCommand;
import work.alsace.buildercore.commands.weather.WeatherCommand;
import work.alsace.buildercore.listeners.*;
import work.alsace.buildercore.features.BuilderTools;
import work.alsace.buildercore.features.cushiontools.CushionTools;
import work.alsace.buildercore.service.HomeDataLoader;
import work.alsace.buildercore.service.TPAHandler;
import work.alsace.buildercore.service.VelocityMessenger;
import work.alsace.buildercore.service.WarpDataLoader;
import work.alsace.buildercore.utils.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BuilderCore extends JavaPlugin {

    public static BuilderCore instance;

    // 内存数据架构：升级为线程安全 Map
    public final Map<String, Boolean> hasIgnored = new ConcurrentHashMap<>();
    public final Map<String, Boolean> hasAgree = new ConcurrentHashMap<>();
    public final Map<UUID, HomeDataLoader> homeProfiles = new ConcurrentHashMap<>();
    public final Map<String, WarpDataLoader> warpProfiles = new ConcurrentHashMap<>();
    public final Set<UUID> openChests = ConcurrentHashMap.newKeySet();

    public static List<String> blockedCommands = List.of("/plugins", "/?", "/help", "/bukkit");
    public static String afkPrefix = "";

    public boolean agreement;
    public String url;
    private String urlTitle;
    public int backHistory;

    private TPAHandler tpaHandler;
    private DataBaseManager databaseManager;
    private LiteDataBaseManager liteDataBaseManager;
    private SerializeInventory serializeInventory;
    private BuilderTools builderTools;
    private CushionTools cushionTools;

    private String host;
    private String dataBase;
    private String userName;
    private String password;

    public static BuilderCore getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        loadConfig();
        tpaHandler = new TPAHandler(this);
        serializeInventory = new SerializeInventory();
        liteDataBaseManager = new LiteDataBaseManager(this);
        if (agreement) {
            databaseManager = new DataBaseManager(this, host, dataBase, userName, password);
            loadPlayerAgreementStatusAsync();
            liteDataBaseManager.connect();
        }
        registerCommands();
        registerListeners();
        builderTools = new BuilderTools(this);
        builderTools.register();
        registerCompletions();
        Bukkit.getScheduler().runTaskTimer(this, new NoClipUtil()::checkBlock, 1L, 1L);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new VelocityMessenger();
        } else {
            getLogger().warning("未检测到 PlaceholderAPI，Tab 列表及拓展变量功能将不生效。");
        }

        if (!Bukkit.getWorlds().isEmpty()) {
            World mainWorld = Bukkit.getWorlds().getFirst();
            mainWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        }

        getLogger().info("BuilderCore 已成功加载！");
    }

    @Override
    public void onDisable() {
        if (cushionTools != null) cushionTools.close();
        homeProfiles.clear();
        warpProfiles.clear();
        if (databaseManager != null) databaseManager.closeDataSource();
        if (liteDataBaseManager != null) liteDataBaseManager.disconnect();

        getLogger().info("BuilderCore 插件已成功卸载。");
    }

    private void registerCompletions() {
        getDescription().getCommands().keySet().forEach(name -> {
            PluginCommand command = getCommand(name);
            TabCompleter delegate = command.getTabCompleter();
            if (delegate == null && command.getExecutor() instanceof TabCompleter completer) delegate = completer;
            command.setTabCompleter(new work.alsace.buildercore.commands.CommandCompletion(this, delegate));
        });
    }

    private void registerCommands() {
        // Player & Basic Commands
        registerCmd("fly", new FlyCommand());
        registerCmd("speed", new SpeedCommand());
        registerCmd("gamemode", new GameModeCommand());
        registerCmd("time", new TimeCommand());
        registerCmd("weather", new WeatherCommand());
        registerCmd("ptime", new PTimeCommand());
        registerCmd("pweather", new PWeatherCommand());
        registerCmd("head", new HeadCommand());
        registerCmd("night", new NightvisionCommand());
        AdvanceFlyCommand advanceFly = new AdvanceFlyCommand();
        registerCmd("advfly", advanceFly);
        getServer().getPluginManager().registerEvents(advanceFly, this);
        registerCmd("debugstick", new DebugStickCommand());
        registerCmd("slab", new SlabCommand());
        registerCmd("noclip", new NoClipCommand());
        registerCmd("reach", new ReachCommand());
        registerCmd("scale", new PlayerScaleCommand());

        // WE / Builder Tools Commands
        registerCmd("/convex", new ConvexCommand());
        registerCmd("/derot", new DerotCommand());
        registerCmd("/cuboid", new CuboidCommand());
        registerCmd("/twist", new TwistCommand());
        registerCmd("/scale", new ScaleCommand());
        registerCmd("undo", new UndoCommand());

        // Utilities & Interactions
        registerCmd("hat", new HatCommand());
        registerCmd("back", new BackCommand(this));
        registerCmd("afk", new AFKCommand(this));

        // Teleport Commands
        registerCmd("tp", new TPCommand(this));
        registerCmd("tphere", new TPHereCommand());
        registerCmd("tpignore", new TPIgnoreCommand(this));
        registerCmd("tpa", new TPACommand(this));
        registerCmd("tpahere", new TPAHereCommand(this));
        registerCmd("tpaccept", new TPAcceptCommand(this));
        registerCmd("tpdeny", new TPADenyCommand(this));

        // Home Commands
        registerCmd("home", new HomeCommand());
        registerCmd("homes", new HomesCommand());
        registerCmd("sethome", new SetHomeCommand());
        registerCmd("delhome", new DelHomeCommand());

        // Warp Commands
        registerCmd("warp", new WarpCommand());
        registerCmd("warps", new WarpsCommand());
        registerCmd("setwarp", new SetWarpCommand());
        registerCmd("delwarp", new DelWarpCommand());

        // Agreement Commands
        if (agreement) {
            registerCmd("agree", new AgreeCommand(this));
            registerCmd("disagree", new DisagreeCommand(this));
        }

        // Admin & Management Commands
        registerCmd("buildercore", new BuilderCoreCommand(this));
        registerCmd("whois", new WhoisCommand());
        registerCmd("itemname", new ItemNameCommand());
        registerCmd("itemnbt", new ItemNbtCommand());
        registerCmd("itemcolor", new ItemColorCommand());
        registerCmd("vanish", new VanishCommand());
        registerCmd("invsee", new InvseeCommand());
        registerCmd("endersee", new EnderseeCommand());

        getLogger().info("所有指令注册完毕。");
    }

    private void registerCmd(String name, Object executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) {
            getLogger().warning("无法注册指令 /" + name + "，请确保 plugin.yml 中已声明该指令！");
            return;
        }
        if (executor instanceof CommandExecutor ce) {
            cmd.setExecutor(ce);
        }
        if (executor instanceof TabCompleter tc) {
            cmd.setTabCompleter(tc);
        }
    }

    private void registerListeners() {
        PluginManager pm = Bukkit.getPluginManager();

        pm.registerEvents(new Protect(), this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new BlockListener(), this);
        cushionTools = new CushionTools(this);
        cushionTools.register();
        pm.registerEvents(new Misc(), this);
        pm.registerEvents(new SpawnListener(), this);
        pm.registerEvents(new CatListener(), this);
        pm.registerEvents(new HorseListener(), this);
        pm.registerEvents(new FoxListener(), this);
        pm.registerEvents(new VanishListener(), this);
        pm.registerEvents(new ChestListener(), this);

        if (pm.isPluginEnabled("PlaceholderAPI")) {
            new Placeholder(this).register();
            new AFKListener(this);
        }
        getLogger().info("所有监听器注册完毕。");
    }

    public void loadConfig() {
        saveDefaultConfig();
        reloadConfig();

        File folder = new File(getDataFolder(), "userdata");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // 预加载在线玩家数据
        for (Player p : Bukkit.getOnlinePlayers()) {
            homeProfiles.put(p.getUniqueId(), new HomeDataLoader(p.getUniqueId()));
        }

        warpProfiles.put("warps", new WarpDataLoader(this));

        ConfigurationSection agreeSec = getConfig().getConfigurationSection("agree");
        if (agreeSec != null) {
            boolean requested = agreeSec.getBoolean("agreement", false);
            if (tpaHandler != null && requested != agreement) {
                getLogger().warning("协议开关变更需要重启服务器；本次重载保留当前运行状态。");
            } else agreement = requested;
            urlTitle = agreeSec.getString("url-title", "");
            url = agreeSec.getString("url", "");
        }

        ConfigurationSection dbSec = getConfig().getConfigurationSection("database");
        if (dbSec != null) {
            host = dbSec.getString("host", "localhost:3306");
            dataBase = dbSec.getString("database", "minecraft");
            userName = dbSec.getString("username", "root");
            password = dbSec.getString("password", "");
        }

        ConfigurationSection afkSec = getConfig().getConfigurationSection("afk");
        if (afkSec != null) {
            afkPrefix = afkSec.getString("placeholder", "[AFK] ");
        }

        backHistory = getConfig().getInt("backHistory", 5);
    }

    /**
     * 异步加载玩家的协议签署状态，避免主线程在服务器启动或重载时卡顿
     */
    private void loadPlayerAgreementStatusAsync() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerUUID = player.getUniqueId();
            hasAgree.put(player.getName(), true);
            databaseManager.hasPlayerAgreed(playerUUID).thenAccept(hasAgreed -> {
                Bukkit.getScheduler().runTask(this, () -> {
                    if (player.isOnline() && !Boolean.FALSE.equals(hasAgree.get(player.getName()))) hasAgree.put(player.getName(), !hasAgreed);
                });
            });
        }
    }

    // Getters
    public DataBaseManager getDatabaseManager() {
        return databaseManager;
    }

    public TPAHandler getTPAHandler() {
        return tpaHandler;
    }

    public SerializeInventory getSerializeInventory() {
        return serializeInventory;
    }

    public LiteDataBaseManager getLiteDataBaseManager() {
        return liteDataBaseManager;
    }

    public String getUrl() {
        return url;
    }

    public String getUrlTitle() {
        return urlTitle;
    }
}
