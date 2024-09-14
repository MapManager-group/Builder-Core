package work.alsace.alsacecore.commands.tools;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InvseeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("alsace.commands.invsee")) {
            sender.sendMessage(Component.text("你没有使用该命令的权限").color(NamedTextColor.RED));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("该命令仅限玩家执行").color(NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(Component.text("参数不足，请补全参数").color(NamedTextColor.RED));
            return true;
        }

        Player target =  Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("未找到玩家" + args[0]).color(NamedTextColor.RED));
            return true;
        }
        sender.sendMessage(Component.text("正在打开").color(NamedTextColor.GRAY)
                .append(Component.text(target.getName()).color(NamedTextColor.WHITE))
                .append(Component.text("的背包...").color(NamedTextColor.GRAY))
        );
        player.openInventory(target.getInventory()).setTitle(target.getName() + "的背包");
        return true;
    }
}
