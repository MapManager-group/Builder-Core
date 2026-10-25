package work.alsace.buildercore.commands.itemEdit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ItemNbtCommand implements CommandExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Component error = MM.deserialize("<gray>正确指令:\n<white>/itemnbt <gray>- 获取手中物品的详细信息");
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] strings) {
        if (sender instanceof Player player) {
            if (!sender.hasPermission("buildercore.commands.itemnbt")) {
                sender.sendMessage(MM.deserialize("<red>你没有使用该命令的权限"));
                return false;
            }

            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand.getType().isAir()) {
                player.sendMessage(MM.deserialize("<red>你手中没有物品"));
                return false;
            }

            String nbt = itemInHand.getItemMeta().toString();
            Component nbtMessage = MM.deserialize("<gray>物品NBT: ").append(Component.text(nbt, net.kyori.adventure.text.format.NamedTextColor.GREEN))
                    .clickEvent(ClickEvent.copyToClipboard(nbt))
                    .hoverEvent(HoverEvent.showText(Component.text("点击复制")));

            player.sendMessage(MM.deserialize("<gray>物品信息"));
            player.sendMessage(MM.deserialize("<gray>物品ID: <white>" + itemInHand.getType()));
            player.sendMessage(MM.deserialize("<gray>物品名称: <white>" + Objects.requireNonNull(itemInHand.getItemMeta()).getDisplayName()));
            player.sendMessage(nbtMessage);
            if (itemInHand.getItemMeta().hasCustomModelData()) {
                player.sendMessage(MM.deserialize("<gray>CustomModelData: <white>" + itemInHand.getItemMeta().getCustomModelData()));
            }
            return true;
        }
        sender.sendMessage(error);
        return false;
    }
}
