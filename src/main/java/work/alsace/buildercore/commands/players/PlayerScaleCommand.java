package work.alsace.buildercore.commands.players;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlayerScaleCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("该命令仅限玩家执行").color(NamedTextColor.RED));
            return true;
        }
        if (!sender.hasPermission("buildercore.commands.scale")) {
            sender.sendMessage(Component.text("你没有使用该命令的权限").color(NamedTextColor.RED));
            return true;
        }
        if (args.length < 1) {
            changeScale(player, 1);
            player.sendMessage(Component.text("已将你的尺寸倍数重置为默认值").color(NamedTextColor.GRAY));
            return true;
        } else {
            double value;
            try {
                value = Double.parseDouble(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text(args[0]+"不是一个有效的数字").color(NamedTextColor.RED));
                return false;
            }
            if (changeScale(player, value)) {
                player.sendMessage(Component.text("已将你的尺寸倍数修改为 ").color(NamedTextColor.GRAY).append(
                        Component.text(args[0]).color(NamedTextColor.WHITE))
                );
                return true;
            } else {
                player.sendMessage(Component.text(args[0]+"不是一个有效的数字").color(NamedTextColor.RED));
                return false;
            }
        }
    }

    private boolean changeScale(Player player, double scale) {
        if (scale < 0.0625 || scale > 16)
            return false;
        player.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(scale);
        return true;
    }
}
