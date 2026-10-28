package work.alsace.buildercore.commands.itemEdit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.jetbrains.annotations.NotNull;

public class ItemColorCommand implements CommandExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Component error = MM.deserialize("<gray>正确指令:\n<white>/itemcolor <六位十六进制颜色> <gray>- 修改手中物品(皮革)的颜色");
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] strings) {
        if (work.alsace.buildercore.commands.CommandHelp.requested(sender, command, label, strings)) return true;

        if (strings.length == 1 && sender instanceof Player player) {
            if (!sender.hasPermission("buildercore.commands.itemcolor")) {
                sender.sendMessage(MM.deserialize("<red>你没有使用该命令的权限"));
                return false;
            }

            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand.getType().isAir()) {
                player.sendMessage(MM.deserialize("<red>你手中没有物品"));
                return false;
            }

            String color = strings[0].replace("#", "");
            if (!isValidColorCode(color)) {
                player.sendMessage(MM.deserialize("<red>无效的颜色代码"));
                return false;
            }

            if (itemInHand.getItemMeta() instanceof LeatherArmorMeta leatherMeta) {
                leatherMeta.setColor(org.bukkit.Color.fromRGB(Integer.parseInt(color, 16)));
                itemInHand.setItemMeta(leatherMeta);
                player.sendMessage(MM.deserialize("<gray>物品颜色已成功修改"));
            } else {
                player.sendMessage(MM.deserialize("<red>该物品不能修改颜色"));
            }
            return true;
        }
        sender.sendMessage(error);
        return false;
    }

    private boolean isValidColorCode(String colorCode) {
        return colorCode.matches("[0-9A-Fa-f]{6}");
    }
}
