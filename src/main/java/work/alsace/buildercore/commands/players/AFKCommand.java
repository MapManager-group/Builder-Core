package work.alsace.buildercore.commands.players;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import work.alsace.buildercore.BuilderCore;

public class AFKCommand implements CommandExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final BuilderCore plugin;

    public AFKCommand(BuilderCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MM.deserialize("<red>该指令仅限玩家执行"));
            return false;
        }

        if (args.length == 0) {
            toggleAFK(player);
            return true;
        } else {
            player.sendMessage(MM.deserialize("<red>正确指令:\n<white>/afk <gray>- 进入暂离状态"));
        }

        return false;
    }

    private void toggleAFK(Player player) {
        if (player.hasMetadata("afk")) {
            player.sendMessage(MM.deserialize("<gray>你已取消暂离状态"));
            player.removeMetadata("afk", plugin);
            Bukkit.broadcast(MM.deserialize("<gray>玩家 <white>" + player.getName() + " <gray>回来了"));
        } else {
            player.sendMessage(MM.deserialize("<gray>你已进入暂离状态"));
            player.setMetadata("afk", new FixedMetadataValue(plugin, true));
            Bukkit.broadcast(MM.deserialize("<gray>玩家 <white>" + player.getName() + " <gray>暂时离开了"));
        }
    }
}
