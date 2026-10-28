package work.alsace.buildercore.commands.players;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class HatCommand implements CommandExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    public HatCommand() {
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (work.alsace.buildercore.commands.CommandHelp.requested(sender, cmd, label, args)) return true;
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MM.deserialize("<red>该指令仅限玩家使用"));
        } else {
            PlayerInventory inventory = player.getInventory();
            ItemStack item = inventory.getItemInMainHand();
            inventory.setItemInMainHand(inventory.getHelmet());
            inventory.setHelmet(item);
        }
        return true;
    }
}
