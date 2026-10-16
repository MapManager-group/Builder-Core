package work.alsace.buildercore.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import work.alsace.buildercore.BuilderCore;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class HomeDataLoader {

    private final UUID playerUUID;
    private final Map<String, Location> homes = new ConcurrentHashMap<>();
    private final File userFile;
    private YamlConfiguration userConfig;
    private int maxHomes = 0;

    public HomeDataLoader(UUID uuid) {
        this.playerUUID = uuid;
        File userDataDir = new File(BuilderCore.instance.getDataFolder(), "userdata");
        if (!userDataDir.exists()) {
            userDataDir.mkdirs();
        }
        this.userFile = new File(userDataDir, playerUUID.toString() + ".yml");

        loadDataSync();
        calculateMaxHomes();
    }

    private void loadDataSync() {
        if (!userFile.exists()) {
            try {
                userFile.createNewFile();
            } catch (IOException e) {
                BuilderCore.instance.getLogger().log(Level.SEVERE, "无法创建玩家配置文件: " + userFile.getName(), e);
            }
        }

        this.userConfig = new YamlConfiguration();
        // 名称允许点号，内部路径改用 NUL 分隔，文件中的 YAML 结构保持不变。
        userConfig.options().pathSeparator('\0');
        try {
            userConfig.load(userFile);
        } catch (IOException | InvalidConfigurationException exception) {
            BuilderCore.instance.getLogger().log(java.util.logging.Level.SEVERE, "无法加载位置数据: " + userFile, exception);
        }
        ConfigurationSection homeSection = userConfig.getConfigurationSection("Home");
        if (homeSection != null) {
            for (String key : homeSection.getKeys(false)) {
                String worldName = homeSection.getString(key + "\0World");
                if (worldName == null) continue;

                World world = Bukkit.getWorld(worldName);
                double x = homeSection.getDouble(key + "\0X");
                double y = homeSection.getDouble(key + "\0Y");
                double z = homeSection.getDouble(key + "\0Z");
                float yaw = (float) homeSection.getDouble(key + "\0Yaw");
                float pitch = (float) homeSection.getDouble(key + "\0Pitch");

                // 允许 Location 中的 World 暂时为 null（防止因世界未加载导致内存丢失）
                homes.put(key.toLowerCase(), new Location(world, x, y, z, yaw, pitch));
            }
        }
    }

    public void calculateMaxHomes() {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) return;

        int calculated = 0;
        for (PermissionAttachmentInfo pInfo : player.getEffectivePermissions()) {
            String perm = pInfo.getPermission().toLowerCase();
            if (perm.equals("buildercore.home.max.unlimited")) {
                this.maxHomes = Integer.MAX_VALUE;
                return;
            }
            if (perm.startsWith("buildercore.home.max.")) {
                try {
                    int val = Integer.parseInt(perm.substring("buildercore.home.max.".length()));
                    if (val > calculated) calculated = val;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        this.maxHomes = calculated;
    }

    public void addHome(String name, Location location) {
        String key = name.toLowerCase();
        homes.put(key, location);

        Bukkit.getScheduler().runTaskAsynchronously(BuilderCore.instance, () -> {
            userConfig.set("Home\0" + key + "\0World", location.getWorld() != null ? location.getWorld().getName() : "");
            userConfig.set("Home\0" + key + "\0X", location.getX());
            userConfig.set("Home\0" + key + "\0Y", location.getY());
            userConfig.set("Home\0" + key + "\0Z", location.getZ());
            userConfig.set("Home\0" + key + "\0Yaw", location.getYaw());
            userConfig.set("Home\0" + key + "\0Pitch", location.getPitch());
            saveConfig();
        });
    }

    public void delHome(String name) {
        String key = name.toLowerCase();
        homes.remove(key);

        Bukkit.getScheduler().runTaskAsynchronously(BuilderCore.instance, () -> {
            userConfig.set("Home\0" + key, null);
            saveConfig();
        });
    }

    private synchronized void saveConfig() {
        try {
            userConfig.save(userFile);
        } catch (IOException e) {
            BuilderCore.instance.getLogger().log(Level.SEVERE, "保存玩家配置文件失败: " + userFile.getName(), e);
        }
    }

    public Location getHome(String name) {
        return homes.get(name.toLowerCase());
    }

    public Set<String> getHomes() {
        return homes.keySet();
    }

    public int getMaxHomes() {
        return maxHomes;
    }
}