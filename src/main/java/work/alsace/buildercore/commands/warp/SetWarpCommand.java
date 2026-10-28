package work.alsace.buildercore.commands.warp;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import work.alsace.buildercore.BuilderCore;
import work.alsace.buildercore.service.WarpDataLoader;

public class SetWarpCommand implements CommandExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (work.alsace.buildercore.commands.CommandHelp.requested(sender, cmd, commandLabel, args)) return true;
        if (!(sender instanceof Player)) {
            sender.sendMessage(MM.deserialize("<red>该指令仅限玩家执行"));
            return false;
        }
        if (!sender.hasPermission("buildercore.commands.setwarp")) {
            sender.sendMessage(MM.deserialize("<red>你没有使用该命令的权限"));
            return false;
        }
        if (args.length == 1 || args.length == 2) {
            String warpName = args[0];
            String alias = (args.length == 2) ? args[1] : null;
            WarpDataLoader warpDataLoader;
            Player player = (Player) sender;
            warpDataLoader = BuilderCore.instance.warpProfiles.get("warps");
            if (warpDataLoader.getWarp(warpName) != null || alias != null && (alias.equals(warpName) || warpDataLoader.getWarp(alias) != null)) {
                sender.sendMessage(MM.deserialize("<red>传送点名称或别名已存在，请使用其他名称。"));
                return false;
            }
            try {
                warpDataLoader.addWarp(warpName, alias, player.getLocation());
            } catch (IllegalArgumentException exception) {
                sender.sendMessage(MM.deserialize("<red>传送点名称或别名已存在，请使用其他名称。"));
                return false;
            } catch (IllegalStateException exception) {
                work.alsace.buildercore.BuilderCore.instance.getLogger().log(java.util.logging.Level.SEVERE, "位置数据保存失败", exception);
                sender.sendMessage(MM.deserialize("<red>保存失败，原文件已保留，请联系管理员查看日志。"));
                return true;
            }
            sender.sendMessage(MM.deserialize(String.format("<gray>成功设置传送点 %s", MM.escapeTags(warpName))));
        } else {
            sender.sendMessage(MM.deserialize("<gray>正确指令:\n<white>/setwarp <传送点> [别名] <gray>- 设置传送点"));
        }
        return true;
    }
}
