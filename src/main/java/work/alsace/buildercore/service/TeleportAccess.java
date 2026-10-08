package work.alsace.buildercore.service;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import studio.lorian.mapmanager.api.MapManager;
import studio.lorian.mapmanager.api.model.MapRole;
import work.alsace.buildercore.BuilderCore;

import java.util.UUID;
import java.util.logging.Level;

/** TP 检查建筑角色，TPA 检查准入；未登记为 MMC 地图的世界不限制。 */
public final class TeleportAccess {
    private TeleportAccess() {}

    public static boolean canTeleport(BuilderCore plugin, Player player, World world, boolean direct) {
        return canTeleport(plugin.getConfig().getBoolean("teleport.check-mmc-permissions", true),
                player.getUniqueId(), world.getName(), direct);
    }

    static boolean canTeleport(boolean checkMmc, UUID playerId, String worldName, boolean direct) {
        if (!checkMmc || !Bukkit.getPluginManager().isPluginEnabled("MapManager-Core")) return true;
        try {
            // 每次查询服务，兼容 MMC 异步初始化及服务撤销。
            MapManager mmc = Bukkit.getServicesManager().load(MapManager.class);
            if (mmc == null) return false;
            var map = mmc.maps().findByWorldName(worldName);
            if (map == null) return true;
            var decision = mmc.access().decide(playerId, map.id());
            return decision.allowed() && (!direct || decision.effectiveRole() == MapRole.ADMIN
                    || decision.effectiveRole() == MapRole.BUILDER);
        } catch (RuntimeException | LinkageError exception) {
            Bukkit.getLogger().log(Level.WARNING, "无法检查 MMC 传送权限：" + worldName, exception);
            return false;
        }
    }
}
