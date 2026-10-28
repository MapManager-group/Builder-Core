package work.alsace.buildercore.commands.agree;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import work.alsace.buildercore.BuilderCore;
import work.alsace.buildercore.utils.DataBaseManager;

import java.util.UUID;

public class DisagreeCommand implements CommandExecutor {

    private final BuilderCore plugin;
    private final DataBaseManager databaseManager;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public DisagreeCommand(BuilderCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (work.alsace.buildercore.commands.CommandHelp.requested(sender, command, label, args)) return true;
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MM.deserialize("<red>只有玩家可以使用这个命令！"));
            return true;
        }

        UUID playerUUID = player.getUniqueId();

        databaseManager.delPlayerAgreed(playerUUID).whenComplete((ignored, error) -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.kick(MM.deserialize("<red>您必须阅读并同意 <yellow>" + plugin.getUrlTitle() + "</yellow> 才能使用服务器的服务！"));
                }
            });
        });

        return true;
    }
}