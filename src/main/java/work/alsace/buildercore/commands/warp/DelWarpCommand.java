package work.alsace.buildercore.commands.warp;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import work.alsace.buildercore.BuilderCore;
import work.alsace.buildercore.service.WarpDataLoader;

import java.util.List;

public class DelWarpCommand implements TabExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (work.alsace.buildercore.commands.CommandHelp.requested(sender, cmd, commandLabel, args)) return true;
        if (!(sender instanceof Player)) {
            sender.sendMessage(MM.deserialize("<red>该指令仅限玩家执行"));
            return false;
        }
        if (!sender.hasPermission("buildercore.commands.delwarp")) {
            sender.sendMessage(MM.deserialize("<red>你没有使用该命令的权限"));
            return false;
        }
        if (args.length == 1) {
            String warpName = args[0];
            WarpDataLoader warpDataLoader = BuilderCore.instance.warpProfiles.get("warps");
            Location location = warpDataLoader.getWarp(warpName);
            if (location == null) {
                sender.sendMessage(MM.deserialize("<red>传送点" + MM.escapeTags(warpName) + "不存在"));
                return false;
            }
            try {
                warpDataLoader.delWarp(warpName);
            } catch (IllegalStateException exception) {
                work.alsace.buildercore.BuilderCore.instance.getLogger().log(java.util.logging.Level.SEVERE, "位置数据保存失败", exception);
                sender.sendMessage(MM.deserialize("<red>保存失败，原文件已保留，请联系管理员查看日志。"));
                return true;
            }
            sender.sendMessage(MM.deserialize("<gray>已删除传送点" + MM.escapeTags(warpName)));
        } else {
            sender.sendMessage(MM.deserialize("<gray>正确指令:\n<white>/delwarp <传送点> <gray>- 删除传送点"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String s, String[] args) {
        if (args.length != 1 || !(sender instanceof Player))
            return List.of();
        return BuilderCore.instance.warpProfiles.get("warps").getWarps().stream()
                .filter(name -> name.startsWith(args[0]))
                .toList();
    }
}
