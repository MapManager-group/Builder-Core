package work.alsace.buildercore.service;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import studio.lorian.mapmanager.api.MapManager;
import studio.lorian.mapmanager.api.model.*;
import studio.lorian.mapmanager.api.service.*;

import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.logging.Logger;

/** 无需服务器的权限矩阵检查；先 test-compile，再用 java -ea 运行此类。 */
public final class TeleportAccessCheck {
    private static <T> T proxy(Class<T> type, Function<String, Object> calls) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (p, method, args) -> calls.apply(method.getName())));
    }

    public static void main(String[] args) throws Exception {
        AtomicBoolean installed = new AtomicBoolean(false);
        AtomicReference<MapManager> provider = new AtomicReference<>();
        PluginManager plugins = proxy(PluginManager.class, name -> installed.get());
        ServicesManager services = proxy(ServicesManager.class, name -> provider.get());
        // 避免 Bukkit.setServer 要求真实 Paper ServerBuildInfo。
        var serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, proxy(Server.class, name -> switch (name) {
            case "getPluginManager" -> plugins;
            case "getServicesManager" -> services;
            case "getLogger" -> Logger.getLogger("TeleportAccessCheck");
            case "getName", "getVersion", "getBukkitVersion" -> "check";
            default -> throw new AssertionError(name);
        }));
        UUID playerId = UUID.randomUUID();
        assert TeleportAccess.canTeleport(true, playerId, "map", true);
        installed.set(true);
        assert TeleportAccess.canTeleport(false, playerId, "map", true);
        assert TeleportAccess.canTeleport(false, playerId, "map", false);
        if (args.length > 0) {
            System.out.println("TeleportAccess without MMC API passed");
            return;
        }
        assert !TeleportAccess.canTeleport(true, playerId, "map", true); // 服务尚未注册

        AtomicReference<MapInstance> map = new AtomicReference<>(new MapInstance(UUID.randomUUID(),
                "map", "group", "map", new MapSettings(true, false, false), MapStatus.ACTIVE, null));
        AtomicReference<AccessDecision> decision = new AtomicReference<>();
        MapService maps = proxy(MapService.class, name -> map.get());
        MapAccessService access = proxy(MapAccessService.class, name -> decision.get());
        provider.set(proxy(MapManager.class, name -> switch (name) {
            case "maps" -> maps;
            case "access" -> access;
            default -> throw new AssertionError(name);
        }));
        for (MapRole role : MapRole.values()) {
            decision.set(new AccessDecision(true, role, null));
            assert TeleportAccess.canTeleport(true, playerId, "map", true)
                    == (role == MapRole.ADMIN || role == MapRole.BUILDER) : role;
            assert TeleportAccess.canTeleport(true, playerId, "map", false) : role;
            decision.set(new AccessDecision(false, role, null));
            assert !TeleportAccess.canTeleport(true, playerId, "map", true) : role;
            assert !TeleportAccess.canTeleport(true, playerId, "map", false) : role;
        }
        decision.set(new AccessDecision(true, null, null)); // 公开地图
        assert !TeleportAccess.canTeleport(true, playerId, "map", true);
        assert TeleportAccess.canTeleport(true, playerId, "map", false);
        decision.set(new AccessDecision(false, null, null));
        assert !TeleportAccess.canTeleport(true, playerId, "map", true);
        assert !TeleportAccess.canTeleport(true, playerId, "map", false);
        map.set(null); // MMC 未管理的世界
        assert TeleportAccess.canTeleport(true, playerId, "world", true);
        assert TeleportAccess.canTeleport(true, playerId, "world", false);
        provider.set(null); // 服务撤销
        assert !TeleportAccess.canTeleport(true, playerId, "map", false);
        System.out.println("TeleportAccess permission matrix passed");
    }
}
