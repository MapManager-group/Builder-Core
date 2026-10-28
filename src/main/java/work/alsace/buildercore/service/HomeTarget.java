package work.alsace.buildercore.service;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import work.alsace.buildercore.BuilderCore;

public record HomeTarget(String ownerName, String homeName, HomeDataLoader loader, boolean isOther) {

    public static HomeTarget parse(CommandSender sender, String input, String otherPermission) {
        if (input.contains(":") && sender.hasPermission(otherPermission)) {
            String[] parts = input.split(":", 2);
            String targetUser = parts[0];
            String homeName = parts[1];
            if (targetUser.isEmpty() || homeName.isEmpty()) return null;

            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayerIfCached(targetUser);
            if (targetPlayer == null) {
                targetPlayer = Bukkit.getOfflinePlayer(targetUser);
            }

            if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
                return null;
            }

            HomeDataLoader loader;
            try {
                loader = BuilderCore.instance.homeProfiles.computeIfAbsent(targetPlayer.getUniqueId(), HomeDataLoader::new);
            } catch (IllegalStateException exception) {
                BuilderCore.instance.getLogger().log(java.util.logging.Level.SEVERE, "无法读取玩家的家数据: " + targetUser, exception);
                return null;
            }

            return new HomeTarget(targetUser, homeName, loader, true);
        } else {
            if (!(sender instanceof org.bukkit.entity.Player player)) return null;
            HomeDataLoader loader = BuilderCore.instance.homeProfiles.get(player.getUniqueId());
            return new HomeTarget(player.getName(), input, loader, false);
        }
    }
}