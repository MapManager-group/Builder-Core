package work.alsace.buildercore.commands.warp;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import work.alsace.buildercore.BuilderCore;
import work.alsace.buildercore.service.WarpDataLoader;

import java.util.ArrayList;
import java.util.List;

public class WarpCommand implements TabExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MM.deserialize("<red>该指令仅限玩家执行"));
            return false;
        }
        if (!sender.hasPermission("buildercore.commands.warp")) {
            sender.sendMessage(MM.deserialize("<red>你没有使用该命令的权限"));
            return false;
        }
        if (args.length == 1) {
            String warpName = args[0];
            WarpDataLoader warpDataLoader = BuilderCore.instance.warpProfiles.get("warps");
            if (warpName == null) {
                sender.sendMessage(MM.deserialize("<red>传送点" + warpName + "不存在"));
                return false;
            }
            Location location = warpDataLoader.getWarp(warpName);
            if (location == null) {
                sender.sendMessage(MM.deserialize("<red>传送点" + warpName + "不存在"));
                return false;
            }
            World warpWorld = location.getWorld();
            if (warpWorld != null && Bukkit.getWorld(warpWorld.getName()) != null) {
                if (sender.hasPermission("multiverse.access." + warpWorld.getName())) {
                    Player player = (Player) sender;
                    player.teleport(location);
                    sender.sendMessage(MM.deserialize("<gray>已传送至" + warpName));
                } else {
                    sender.sendMessage(MM.deserialize("<red>你没有权限传送至" + warpName));
                    return false;
                }
            } else {
                sender.sendMessage(MM.deserialize("<red>世界不存在或未加载"));
                //warpDataLoader.delWarp(warpName);
                return false;
            }
        } else {
            sender.sendMessage(MM.deserialize("<gray>正确指令:\n<white>/warp <传送点> <gray>- 传送至指定传送点"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String s, String[] args) {
        if (args.length != 1)
            return new ArrayList<>(0);
        return new ArrayList<>(getAccessWarps((Player) sender));

    }

    private List<String> getAccessWarps(Player sender) {
        List<String> warps = new ArrayList<>();
        BuilderCore.instance.warpProfiles.get("warps").getWarps().forEach(warp -> {
            Location location = BuilderCore.instance.warpProfiles.get("warps").getWarp(warp);
            World world = location.getWorld();
            if (world != null) {
                if (sender.hasPermission("multiverse.access." + world.getName())) {
                    warps.add(warp);
                }
            }
        });
        return warps;
    }
}
