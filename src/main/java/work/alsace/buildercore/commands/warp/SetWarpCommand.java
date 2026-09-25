package work.alsace.buildercore.commands.warp;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import work.alsace.buildercore.BuilderCore;
import work.alsace.buildercore.service.WarpDataLoader;

import java.util.regex.Pattern;

public class SetWarpCommand implements CommandExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MM.deserialize("<red>该指令仅限玩家执行"));
            return false;
        }
        if (!sender.hasPermission("buildercore.commands.setwarp")) {
            sender.sendMessage(MM.deserialize("<red>你没有使用该命令的权限"));
            return false;
        }
        if (args.length >= 1) {
            String warpName = args[0];
            String alias = (args.length == 2) ? args[1] : null;  // 获取别名，如果提供的话
            WarpDataLoader warpDataLoader;
            Player player = (Player) sender;
            warpDataLoader = BuilderCore.instance.warpProfiles.get("warps");
            if (warpDataLoader.getWarp(warpName) != null) {
                sender.sendMessage(MM.deserialize("<red>已经存在传送点" + warpName));
                return false;
            }
            String separator = BuilderCore.instance.getConfig().getString("illegal-characters");
            Pattern pattern = null;
            if (separator != null) {
                pattern = Pattern.compile(separator);
            }
            if (pattern != null) {
                for (String i : warpName.split(pattern.pattern())) {
                    if (pattern.matcher(i).matches()) {
                        sender.sendMessage(MM.deserialize("<red>传送点名称包含非法字符"));
                        return false;
                    }
                }
            }
            warpDataLoader.addWarp(warpName, alias, player.getLocation());
            sender.sendMessage(MM.deserialize(String.format("<gray>成功设置传送点 %s", warpName)));
        } else {
            sender.sendMessage(MM.deserialize("<gray>正确指令:\n<white>/setwarp <传送点> <gray>- 设置传送点"));
        }
        return true;
    }
}
