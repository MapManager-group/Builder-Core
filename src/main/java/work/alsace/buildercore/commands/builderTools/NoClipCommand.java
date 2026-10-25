package work.alsace.buildercore.commands.builderTools;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import work.alsace.buildercore.utils.NoClipUtil;

public class NoClipCommand implements CommandExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) {
            return false;
        } else if (!sender.hasPermission("buildercore.commands.noclip")) {
            return false;
        } else {
            Player player = (Player) sender;
            if (NoClipUtil.noclip.contains(player)) {
                NoClipUtil.noclip.remove(player);
                player.sendMessage(MM.deserialize("<gray>已禁用自动穿墙"));
            } else {
                NoClipUtil.noclip.add(player);
                player.sendMessage(MM.deserialize("<gray>已启用自动穿墙"));
            }
            return true;
        }
    }
}
