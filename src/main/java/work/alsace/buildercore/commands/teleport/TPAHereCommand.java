package work.alsace.buildercore.commands.teleport;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import work.alsace.buildercore.BuilderCore;
import work.alsace.buildercore.service.TPAHandler;

public class TPAHereCommand implements CommandExecutor {
    private final BuilderCore plugin;
    private final TPAHandler tpaHandler;

    public TPAHereCommand(BuilderCore plugin) {
        this.plugin = plugin;
        this.tpaHandler = plugin.getTPAHandler();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "该指令仅限玩家执行");
            return false;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("buildercore.commands.tpa")) {
            player.sendMessage(ChatColor.RED + "你没有使用该命令的权限");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "正确指令:\n§f/tpahere <玩家> §7- 请求该玩家传送到你的位置");
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "玩家 " + args[0] + " 不在线。");
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(ChatColor.RED + "你不能向自己发送传送请求");
            return true;
        }

        // 发送 TPAHere 请求 (true 表示将目标拉过来)
        tpaHandler.sendTPARequest(player, target, true);
        return true;
    }
}