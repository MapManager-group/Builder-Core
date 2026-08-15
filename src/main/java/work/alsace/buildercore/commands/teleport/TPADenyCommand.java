package work.alsace.buildercore.commands.teleport;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import work.alsace.buildercore.BuilderCore;
import work.alsace.buildercore.service.TPAHandler;

public class TPADenyCommand implements CommandExecutor {
    private final TPAHandler tpaHandler;

    public TPADenyCommand(BuilderCore plugin) {
        this.tpaHandler = plugin.getTPAHandler();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "该指令仅限玩家执行");
            return false;
        }

        if (!player.hasPermission("buildercore.commands.tpa")) {
            player.sendMessage(ChatColor.RED + "你没有使用该命令的权限");
            return true;
        }

        tpaHandler.denyTPA(player);
        return true;
    }
}