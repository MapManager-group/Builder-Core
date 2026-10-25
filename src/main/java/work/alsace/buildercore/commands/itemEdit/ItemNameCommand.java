package work.alsace.buildercore.commands.itemEdit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class ItemNameCommand implements CommandExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final Component error = MM.deserialize("<gray>正确指令:\n<white>/itemname <物品名称> <gray>- 修改手中物品名称");
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] strings) {
        if (strings.length == 1 && sender instanceof Player player) {
            if (!sender.hasPermission("buildercore.commands.itemname")) {
                sender.sendMessage(MM.deserialize("<red>你没有使用该命令的权限"));
                return false;
            }

            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand.getType().isAir()) {
                player.sendMessage(MM.deserialize("<red>你手中没有物品"));
                return false;
            }

            String itemName = strings[0].replace('&', '§');

            ItemMeta itemMeta = itemInHand.getItemMeta();
            itemMeta.displayName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(itemName));
            itemInHand.setItemMeta(itemMeta);
            player.sendMessage(MM.deserialize("<gray>已将手中物品名称修改为<reset> ").append(itemMeta.displayName()));
            return true;
        }
        sender.sendMessage(error);
        return false;
    }
}
