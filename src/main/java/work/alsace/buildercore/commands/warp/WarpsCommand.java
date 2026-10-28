// WarpsCommand.java
package work.alsace.buildercore.commands.warp;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import work.alsace.buildercore.BuilderCore;
import work.alsace.buildercore.service.WarpDataLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WarpsCommand implements CommandExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (work.alsace.buildercore.commands.CommandHelp.requested(sender, cmd, commandLabel, args)) return true;
        if (!(sender instanceof Player)) {
            sender.sendMessage(MM.deserialize("<red>该指令仅限玩家执行"));
            return false;
        }
        if (!sender.hasPermission("buildercore.commands.warps")) {
            sender.sendMessage(MM.deserialize("<red>你没有使用该命令的权限"));
            return false;
        }

        Player player = (Player) sender;
        WarpDataLoader warpDataLoader = BuilderCore.instance.warpProfiles.get("warps");

        Map<String, List<String>> worldWarps = getWorldWarps(player);

        if (worldWarps.isEmpty()) {
            player.sendMessage(MM.deserialize("<red>没有可用的传送点"));
            return false;
        }

        player.sendMessage(MM.deserialize("<gray>可用传送点:"));

        for (Map.Entry<String, List<String>> entry : worldWarps.entrySet()) {
            String worldName = entry.getKey();
            List<String> warps = entry.getValue();
            Component message = MM.deserialize("<gray>" + worldName + ": ");
            for (String warp : warps) {
                Component warpComponent = createClickableWarpComponent(warp);
                message = message.append(warpComponent);
                message = message.append(MM.deserialize(" <gray>| "));
            }

            player.sendMessage(message);
        }

        return true;
    }

    private Map<String, List<String>> getWorldWarps(Player sender) {
        Map<String, List<String>> worldWarps = new HashMap<>();
        BuilderCore.instance.warpProfiles.get("warps").getWarps().forEach(warp -> {
            World world = BuilderCore.instance.warpProfiles.get("warps").getWarpWorld(warp);

            if (world == null || Bukkit.getWorld(world.getName()) == null) {
                //BuilderCore.instance.warpProfiles.get("warps").delWarp(warp);
                return;
            }

            String worldName = world.getName();
            String alias = BuilderCore.instance.warpProfiles.get("warps").getWarpAlias(warp);

            worldWarps.computeIfAbsent(worldName, k -> new ArrayList<>()).add(alias != null ? alias : warp);
        });
        return worldWarps;
    }

    private Component createClickableWarpComponent(String warp) {
        String realWarp = BuilderCore.instance.warpProfiles.get("warps").getRealWarpName(warp);

        return Component.text(warp, net.kyori.adventure.text.format.NamedTextColor.GOLD)
                .clickEvent(ClickEvent.runCommand("/warp " + realWarp))
                .hoverEvent(HoverEvent.showText(Component.text("点击快捷传送")));
    }
}
