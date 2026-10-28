package work.alsace.buildercore.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import work.alsace.buildercore.BuilderCore;

import java.io.File;
import java.util.*;

public class WarpDataLoader {
    private final Map<String, Location> warps = new HashMap<>();
    private final Map<String, String> aliases = new HashMap<>();
    private final File warpsFile;
    private YamlConfiguration warpConfig;

    public WarpDataLoader(BuilderCore plugin) {
        warpsFile = new File(plugin.getDataFolder(), "warps.yml");
        warpConfig = LocationStorage.load(warpsFile);
        ConfigurationSection section = warpConfig.getConfigurationSection("Warps");
        if (section != null) for (String name : section.getKeys(false)) {
            String path = name + "\0";
            String world = section.getString(path + "World");
            if (world == null) continue;
            warps.put(name, new Location(Bukkit.getWorld(world), section.getDouble(path + "X"),
                    section.getDouble(path + "Y"), section.getDouble(path + "Z"),
                    (float) section.getDouble(path + "Yaw"), (float) section.getDouble(path + "Pitch")));
        }
        ConfigurationSection aliasSection = warpConfig.getConfigurationSection("Aliases");
        if (aliasSection != null) for (String alias : aliasSection.getKeys(false)) {
            String name = aliasSection.getString(alias);
            if (warps.containsKey(name)) aliases.put(alias, name);
        }
    }

    public synchronized void addWarp(String name, String alias, Location location) {
        YamlConfiguration next = LocationStorage.load(warpsFile);
        String path = "Warps\0" + name + "\0";
        next.set(path + "World", Objects.requireNonNull(location.getWorld()).getName());
        next.set(path + "X", location.getX());
        next.set(path + "Y", location.getY());
        next.set(path + "Z", location.getZ());
        next.set(path + "Yaw", location.getYaw());
        next.set(path + "Pitch", location.getPitch());
        if (alias != null && !alias.isEmpty()) next.set("Aliases\0" + alias, name);
        LocationStorage.save(warpsFile, next);
        warpConfig = next;
        warps.put(name, location);
        if (alias != null && !alias.isEmpty()) aliases.put(alias, name);
    }

    public synchronized void delWarp(String name) {
        String realName = warps.containsKey(name) ? name : getRealWarpName(name);
        YamlConfiguration next = LocationStorage.load(warpsFile);
        next.set("Warps\0" + realName, null);
        aliases.forEach((alias, target) -> {
            if (target.equals(realName)) next.set("Aliases\0" + alias, null);
        });
        LocationStorage.save(warpsFile, next);
        warpConfig = next;
        warps.remove(realName);
        aliases.values().removeIf(realName::equals);
    }

    public synchronized Location getWarp(String nameOrAlias) {
        String name = warps.containsKey(nameOrAlias) ? nameOrAlias : getRealWarpName(nameOrAlias);
        Location stored = warps.get(name);
        if (stored == null) return null;
        Location location = stored;
        if (location.getWorld() == null) location.setWorld(Bukkit.getWorld(warpConfig.getString("Warps\0" + name + "\0World", "")));
        return location;
    }

    public synchronized Set<String> getWarps() { return new HashSet<>(warps.keySet()); }

    public World getWarpWorld(String name) {
        Location location = getWarp(name);
        return location == null ? null : location.getWorld();
    }

    public synchronized String getWarpAlias(String name) {
        return aliases.entrySet().stream().filter(entry -> entry.getValue().equals(name))
                .map(Map.Entry::getKey).findFirst().orElse(null);
    }

    public synchronized String getRealWarpName(String alias) { return aliases.getOrDefault(alias, alias); }
}
