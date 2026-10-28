package work.alsace.buildercore.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import work.alsace.buildercore.BuilderCore;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HomeDataLoader {

    private final UUID playerUUID;
    private final Map<String, Location> homes = new ConcurrentHashMap<>();
    private final Map<String, String> storedNames = new java.util.HashMap<>();
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
        this.userConfig = LocationStorage.load(userFile);
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
                String normalized = key.toLowerCase(java.util.Locale.ROOT);
                homes.put(normalized, new Location(world, x, y, z, yaw, pitch));
                storedNames.put(normalized, key);
            }
        }
    }

    public void calculateMaxHomes() {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) return;

        int calculated = 0;
        for (PermissionAttachmentInfo pInfo : player.getEffectivePermissions()) {
            if (!pInfo.getValue()) continue;
            String perm = pInfo.getPermission().toLowerCase(java.util.Locale.ROOT);
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
        String key = name.toLowerCase(java.util.Locale.ROOT);

        YamlConfiguration next = LocationStorage.load(userFile);
        next.set("Home\0" + storedNames.getOrDefault(key, key), null);
        next.set("Home\0" + key + "\0World", location.getWorld() != null ? location.getWorld().getName() : "");
        next.set("Home\0" + key + "\0X", location.getX());
        next.set("Home\0" + key + "\0Y", location.getY());
        next.set("Home\0" + key + "\0Z", location.getZ());
        next.set("Home\0" + key + "\0Yaw", location.getYaw());
        next.set("Home\0" + key + "\0Pitch", location.getPitch());
        LocationStorage.save(userFile, next);
        userConfig = next;
        homes.put(key, location);
        storedNames.put(key, key);
    }

    public void delHome(String name) {
        String key = name.toLowerCase(java.util.Locale.ROOT);
        YamlConfiguration next = LocationStorage.load(userFile);
        next.set("Home\0" + storedNames.getOrDefault(key, key), null);
        LocationStorage.save(userFile, next);
        userConfig = next;
        homes.remove(key);
        storedNames.remove(key);
    }

    public Location getHome(String name) {
        String key = name.toLowerCase(java.util.Locale.ROOT);
        Location stored = homes.get(key);
        if (stored == null) return null;
        Location location = stored;
        if (location.getWorld() == null) location.setWorld(Bukkit.getWorld(userConfig.getString("Home\0" + storedNames.getOrDefault(key, key) + "\0World", "")));
        return location;
    }

    public Set<String> getHomes() {
        return homes.keySet();
    }

    public int getMaxHomes() {
        return maxHomes;
    }
}