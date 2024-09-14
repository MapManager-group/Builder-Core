package work.alsace.alsacecore.commands.players;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReachCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("该命令仅限玩家执行").color(NamedTextColor.RED));
            return true;
        }
        if (!sender.hasPermission("alsace.commands.reach")) {
            sender.sendMessage(Component.text("你没有使用该命令的权限").color(NamedTextColor.RED));
            return true;
        }
        if (args.length < 1) {
            changeReach(player, 4.5);
            player.sendMessage(Component.text("已将方块交互距离重置为默认值").color(NamedTextColor.GRAY));
            return true;
        } else {
            double value;
            try {
                value = Double.parseDouble(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text(args[0]+"不是一个有效的数字").color(NamedTextColor.RED));
                return false;
            }
            if (changeReach(player, value)) {
                player.sendMessage(Component.text("已将你的方块交互距离修改为 ").color(NamedTextColor.GRAY).append(
                        Component.text(args[0]).color(NamedTextColor.WHITE))
                );
                return true;
            } else {
                player.sendMessage(Component.text(args[0]+"不是一个有效的数字").color(NamedTextColor.RED));
                return false;
            }
        }
    }

    private boolean changeReach(Player player, double distance) {
        if (distance < 0 || distance > 64)
            return false;
        player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE).setBaseValue(distance);
        return true;
    }
}
