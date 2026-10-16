package work.alsace.buildercore.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;
import work.alsace.buildercore.BuilderCore;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WarpDataLoader {
    private final Map<String, Location> warps = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, String> aliases = Collections.synchronizedMap(new HashMap<>());
    private final File warpsFile;
    private YamlConfiguration warpConfig;

    public WarpDataLoader(BuilderCore plugin) {
        this.warpsFile = new File(plugin.getDataFolder() + File.separator + "warps.yml");
        this.loadWarps();
    }

    private synchronized void loadWarps() {
        if (!warpsFile.exists()) {
            try {
                warpsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        warpConfig = new YamlConfiguration();
        // 名称允许点号，内部路径改用 NUL 分隔，文件中的 YAML 结构保持不变。
        warpConfig.options().pathSeparator('\0');
        try {
            warpConfig.load(warpsFile);
        } catch (IOException | InvalidConfigurationException exception) {
            BuilderCore.instance.getLogger().log(java.util.logging.Level.SEVERE, "无法加载位置数据: " + warpsFile, exception);
        }

        if (warpConfig.getConfigurationSection("Warps") != null) {
            for (String i : warpConfig.getConfigurationSection("Warps").getKeys(false)) {
                warps.put(i, new Location(
                        Bukkit.getWorld(Objects.requireNonNull(warpConfig.getString("Warps\0" + i + "\0World"))),
                        warpConfig.getDouble("Warps\0" + i + "\0X"),
                        warpConfig.getDouble("Warps\0" + i + "\0Y"),
                        warpConfig.getDouble("Warps\0" + i + "\0Z"),
                        warpConfig.getLong("Warps\0" + i + "\0Yaw"),
                        warpConfig.getLong("Warps\0" + i + "\0Pitch")));

                // 加载别名
                String alias = warpConfig.getString("Aliases\0" + i);
                if (alias != null) {
                    aliases.put(alias, i);
                }
            }
        }
    }

    public void addWarp(String name, String alias, Location location) {
        synchronized (warps) {
            warps.put(name, location);
            warpConfig.set("Warps\0" + name + "\0World", Objects.requireNonNull(location.getWorld()).getName());
            warpConfig.set("Warps\0" + name + "\0X", location.getX());
            warpConfig.set("Warps\0" + name + "\0Y", location.getY());
            warpConfig.set("Warps\0" + name + "\0Z", location.getZ());
            warpConfig.set("Warps\0" + name + "\0Yaw", location.getYaw());
            warpConfig.set("Warps\0" + name + "\0Pitch", location.getPitch());

            if (alias != null && !alias.isEmpty()) {
                warpConfig.set("Aliases\0" + alias, name);
                aliases.put(alias, name);
            }

            try {
                warpConfig.save(warpsFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void delWarp(String name) {
        synchronized (warps) {
            warps.remove(name);
            warpConfig.set("Warps\0" + name, null);

            // 删除别名
            String alias = getWarpAlias(name);
            if (alias != null) {
                warpConfig.set("Aliases\0" + alias, null);
                aliases.remove(alias);
            }

            try {
                warpConfig.save(warpsFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Location getWarp(String nameOrAlias) {
        synchronized (warps) {
            Location location = warps.get(nameOrAlias);
            if (location == null) {
                String realName = aliases.get(nameOrAlias);
                if (realName != null) {
                    location = warps.get(realName);
                }
            }
            return location;
        }
    }

    public Set<String> getWarps() {
        return new HashSet<>(warps.keySet());
    }

    public World getWarpWorld(String name) {
        Location location = getWarp(name);
        return (location != null) ? location.getWorld() : null;
    }

    public String getWarpAlias(String name) {
        return aliases.get(name);
    }

    public String getRealWarpName(String alias) {
        return aliases.getOrDefault(alias, alias);
    }
}
