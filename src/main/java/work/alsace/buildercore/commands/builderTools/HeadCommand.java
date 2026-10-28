package work.alsace.buildercore.commands.builderTools;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class HeadCommand implements CommandExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Component error = MM.deserialize("<gray>正确指令:\n<white>/head <玩家名称> <gray>- 获取指定玩家的皮肤头颅");
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] strings) {
        if (work.alsace.buildercore.commands.CommandHelp.requested(sender, command, label, strings)) return true;
        if (strings.length == 1 && sender instanceof Player player) {
            if (!player.hasPermission("buildercore.commands.head")) {
                player.sendMessage(MM.deserialize("<red>你没有使用该命令的权限"));
                return false;
            }
            ItemStack head = createPlayerHead(strings[0]);
            player.getInventory().addItem(head);
            player.sendMessage(MM.deserialize("<gray>成功获得了玩家 <white>" + strings[0] + " <gray>的头颅"));
            return true;
        }
        sender.sendMessage(error);
        return false;
    }

    private ItemStack createPlayerHead(String playerName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
        head.setItemMeta(meta);
        return head;
    }
}
