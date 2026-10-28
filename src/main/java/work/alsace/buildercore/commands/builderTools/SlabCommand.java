package work.alsace.buildercore.commands.builderTools;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import work.alsace.buildercore.listeners.BlockListener;

public class SlabCommand implements CommandExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    public SlabCommand() {
    }

    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (work.alsace.buildercore.commands.CommandHelp.requested(sender, command, s, strings)) return true;
        if (!(sender instanceof Player)) {
            return false;
        } else if (!sender.hasPermission("buildercore.commands.slab")) {
            return false;
        } else {
            Player player = (Player) sender;
            if (BlockListener.slabs.contains(player)) {
                BlockListener.slabs.remove(player);
                player.sendMessage(MM.deserialize("<gray>已禁用半砖破坏模式"));
            } else {
                BlockListener.slabs.add(player);
                player.sendMessage(MM.deserialize("<gray>已启用半砖破坏模式"));
            }

            return true;
        }
    }
}
