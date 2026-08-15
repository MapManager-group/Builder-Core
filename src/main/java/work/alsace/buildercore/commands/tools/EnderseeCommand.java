package work.alsace.buildercore.commands.tools;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import work.alsace.buildercore.BuilderCore;

public class EnderseeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("该命令仅限玩家执行").color(NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            if (!sender.hasPermission("buildercore.commands.endersee.self")) {
                sender.sendMessage(Component.text("你没有使用该命令的权限").color(NamedTextColor.RED));
                return true;
            }
            sender.sendMessage(Component.text("正在打开末影箱...").color(NamedTextColor.GRAY));
            player.playSound(player, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.BLOCKS, 1.0f, 1.0f);
            player.openInventory(player.getEnderChest());
            BuilderCore.instance.openChests.add(player.getUniqueId());
        } else {
            if (!sender.hasPermission("buildercore.commands.endersee.other")) {
                sender.sendMessage(Component.text("你没有使用该命令的权限").color(NamedTextColor.RED));
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text("未找到玩家"+args[0]).color(NamedTextColor.RED));
                return true;
            }
            sender.sendMessage(Component.text("正在打开").color(NamedTextColor.GRAY)
                    .append(Component.text(target.getName()).color(NamedTextColor.WHITE))
                    .append(Component.text("的末影箱...").color(NamedTextColor.GRAY))
            );
            player.openInventory(target.getEnderChest()).setTitle(target.getName()+"的末影箱");
        }
        return true;
    }
}
