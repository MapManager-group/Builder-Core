package work.alsace.buildercore.commands.builderTools;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DebugStickCommand implements CommandExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (work.alsace.buildercore.commands.CommandHelp.requested(sender, command, s, strings)) return true;
        if (!(sender instanceof Player)) {
            return false;
        } else if (!sender.hasPermission("buildercore.commands.debugstick")) {
            return false;
        } else {
            Player player = (Player) sender;
            ItemStack itemStack = new ItemStack(Material.DEBUG_STICK, 1);
            player.getInventory().addItem(itemStack);
            player.sendMessage(MM.deserialize("<gray>你获得了一个调试棒"));
            return true;
        }
    }
}
