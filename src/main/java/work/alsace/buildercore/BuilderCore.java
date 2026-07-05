package work.alsace.buildercore;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import work.alsace.buildercore.commands.agree.AgreeCommand;
import work.alsace.buildercore.commands.agree.DisagreeCommand;
import work.alsace.buildercore.commands.tools.*;
import work.alsace.buildercore.utils.*;
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
import work.alsace.buildercore.commands.warp.DelWarpCommand;
import work.alsace.buildercore.commands.warp.SetWarpCommand;
import work.alsace.buildercore.commands.warp.WarpCommand;
import work.alsace.buildercore.commands.warp.WarpsCommand;
import work.alsace.buildercore.commands.weather.PWeatherCommand;
import work.alsace.buildercore.commands.weather.WeatherCommand;
import work.alsace.buildercore.listeners.*;
import work.alsace.buildercore.service.HomeDataLoader;
import work.alsace.buildercore.service.TPAHandler;
import work.alsace.buildercore.service.WarpDataLoader;
import work.alsace.buildercore.service.VelocityMessenger;

import java.io.File;
import java.util.*;

public class BuilderCore extends JavaPlugin {

    public static BuilderCore instance;
    public Map<String, Boolean> hasIgnored = new HashMap<>();
    public Map<String, Boolean> hasAgree = new HashMap<>();
    public boolean agreement;
    public String url;
    private String urlTitle;
    public HashMap<UUID, HomeDataLoader> homeProfiles = new HashMap<>();
    public HashMap<String, WarpDataLoader> warpProfiles = new HashMap<>();
    public final Set<UUID> openChests = new HashSet<>();
    private TPAHandler tpaHandler;

    public List<String> illegalCharacters = new ArrayList<>();
    public int backHistory;
    private DataBaseManager databaseManager;

    private String host;
    private String dataBase;
    private String userName;
    private String password;

    public static String afkPrefix;
    public static List<String> blockedCommands;

    private SerializeInventory serializeInventory;
    private LiteDataBaseManager liteDataBaseManager;

    @Override
    public void onEnable() {
        loadConfig();
        tpaHandler = new TPAHandler(this);
        serializeInventory = new SerializeInventory();
        liteDataBaseManager = new LiteDataBaseManager(this);
        if (agreement) {
            databaseManager = new DataBaseManager(host, dataBase, userName, password);
            loadPlayerAgreementStatus();
            liteDataBaseManager.connect();
        }
        registerCommands();
        registerListeners();
        Bukkit.getScheduler().runTaskTimer(this, () -> new NoClipUtil().checkBlock(), 1L, 1L);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new VelocityMessenger();
        } else {
            getLogger().warning("前置插件 PlaceholderAPI 不存在，Tab列表扩展功能失效");
        }

        getLogger().info("插件已完全加载完成");
    }

    @Override
    public void onDisable() {
        homeProfiles.clear();
        warpProfiles.clear();
        if (agreement) {
            if (databaseManager != null) {
                databaseManager.closeDataSource();
            }
            liteDataBaseManager.disconnect();
        }
        getLogger().info("插件已卸载");
    }

    private void registerCommands() {
        BuilderCore.instance = this;
        registerCommand("fly", new FlyCommand());
        registerCommand("speed", new SpeedCommand());
        registerCommand("gamemode", new GameModeCommand());
        registerCommand("time", new TimeCommand());
        registerCommand("weather", new WeatherCommand());
        registerCommand("ptime", new PTimeCommand());
        registerCommand("pweather", new PWeatherCommand());
        registerCommand("head", new HeadCommand());
        registerCommand("night", new NightvisionCommand());
        registerCommand("advfly", new AdvanceFlyCommand());
        registerCommand("debugstick", new DebugStickCommand());
        registerCommand("slab", new SlabCommand());
        registerCommand("noclip", new NoClipCommand());
        registerCommand("reach", new ReachCommand());
        registerCommand("scale", new PlayerScaleCommand());


        registerCommand("/convex", new ConvexCommand());
        registerCommand("/derot", new DerotCommand());
        registerCommand("/cuboid", new CuboidCommand());
        registerCommand("/twist", new TwistCommand());
        registerCommand("/scale", new ScaleCommand());
        registerCommand("undo", new UndoCommand());

        registerCommand("hat", new HatCommand());
        registerCommand("back", new BackCommand(this));
        registerCommand("afk", new AFKCommand(this));

        registerCommand("tp", new TPCommand(this));
        registerCommand("tphere", new TPHereCommand());
        registerCommand("tpignore", new TPIgnoreCommand(this));
        registerCommand("tpahere", new TPACommand(this));
        registerCommand("tpaccept", new TPACommand(this));
        registerCommand("tpdeny", new TPACommand(this));

        registerCommand("home", new HomeCommand());
        registerCommand("homes", new HomesCommand());
        registerCommand("sethome", new SetHomeCommand());
        registerCommand("delhome", new DelHomeCommand());

        registerCommand("warp", new WarpCommand());
        registerCommand("warps", new WarpsCommand());
        registerCommand("setwarp", new SetWarpCommand());
        registerCommand("delwarp", new DelWarpCommand());

        if (agreement) {
            registerCommand("agree", new AgreeCommand(this));
            registerCommand("disagree", new DisagreeCommand(this));
        }

        registerCommand("buildercore", new AlsaceCoreCommand(this));

        registerCommand("whois", new WhoisCommand());
        registerCommand("itemname", new ItemNameCommand());
        registerCommand("itemnbt", new ItemNbtCommand());
        registerCommand("itemcolor", new ItemColorCommand());
        registerCommand("vanish", new VanishCommand());
        registerCommand("invsee", new InvseeCommand());
        registerCommand("endersee", new EnderseeCommand());

        blockedCommands = Arrays.asList("/plugins", "/?", "/help", "/bukkit");
        getLogger().info("指令注册完成");
    }

    private void registerCommand(String command, CommandExecutor executor) {
        Objects.requireNonNull(getCommand(command)).setExecutor(executor);
    }

    private void registerCommand(String command, TabExecutor executor) {
        Objects.requireNonNull(getCommand(command)).setExecutor(executor);
        Objects.requireNonNull(getCommand(command)).setTabCompleter(executor);
    }

    private void registerListeners() {
        BuilderCore.instance = this;
        PluginManager manager = Bukkit.getPluginManager();
        manager.registerEvents(new Protect(), this);
        manager.registerEvents(new PlayerListener(this), this);
        manager.registerEvents(new BlockListener(), this);
        manager.registerEvents(new Misc(), this);
        manager.registerEvents(new AdvanceFlyCommand(), this);
        manager.registerEvents(new SpawnListener(), this);
        manager.registerEvents(new CatListener(), this);
        manager.registerEvents(new HorseListener(), this);
        manager.registerEvents(new FoxListener(), this);
        manager.registerEvents(new VanishListener(), this);
        manager.registerEvents(new ChestListener(), this);
        manager.registerEvents(new InventoryListener(this), this);


        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new Placeholder(this).register();
            getServer().getPluginManager().registerEvents(new AFKListener(this), this);
        } else {
            getLogger().warning("前置插件 PlaceholderAPI 不存在，Afk功能注册失败");
        }

        getLogger().info("事件注册完成");
    }

    public void loadConfig() {
        saveDefaultConfig();
        reloadConfig();
        if (this.getConfig().getConfigurationSection("illegal-characters") != null) {
            for (String i : this.getConfig().getStringList("illegal-characters")) {
                illegalCharacters.add(i.toLowerCase());
            }
        }
        File folder = new File(getDataFolder(), "userdata");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        for (Player i : Bukkit.getServer().getOnlinePlayers()) {
            homeProfiles.put(i.getUniqueId(), new HomeDataLoader(i.getUniqueId()));
        }
        warpProfiles.put("warps", new WarpDataLoader(this));
        agreement = this.getConfig().getConfigurationSection("agree").getBoolean("agreement");
        urlTitle = this.getConfig().getConfigurationSection("agree").getString("url-title");
        url = this.getConfig().getConfigurationSection("agree").getString("url");
        ConfigurationSection dbConfig = this.getConfig().getConfigurationSection("database");
        host = Objects.requireNonNull(dbConfig).getString("host");
        dataBase = dbConfig.getString("database");
        userName = dbConfig.getString("username");
        password = dbConfig.getString("password");
        afkPrefix = Objects.requireNonNull(this.getConfig().getConfigurationSection("afk")).getString("placeholder");
        backHistory = this.getConfig().getInt("backHistory");
    }

    public DataBaseManager getDatabaseManager() {
        return databaseManager;
    }

    private void loadPlayerAgreementStatus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerUUID = player.getUniqueId();
            boolean hasAgreed = databaseManager.hasPlayerAgreed(playerUUID);
            if (hasAgreed) {
                // 如果数据库中有同意记录，将其标记为已同意
                this.hasAgree.put(player.getName(), Boolean.FALSE);
            } else {
                // 如果数据库中没有同意记录，将其标记为待同意
                this.hasAgree.put(player.getName(), Boolean.TRUE);
            }
        }
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
