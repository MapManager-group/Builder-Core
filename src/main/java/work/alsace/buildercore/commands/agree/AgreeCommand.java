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

public class AgreeCommand implements CommandExecutor {

    private final BuilderCore plugin;
    private final DataBaseManager databaseManager;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public AgreeCommand(BuilderCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MM.deserialize("<red>只有玩家可以使用这个命令！"));
            return true;
        }

        UUID playerUUID = player.getUniqueId();

        databaseManager.hasPlayerAgreed(playerUUID).thenAccept(hasAgreed -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;

                if (!hasAgreed) {
                    databaseManager.setPlayerAgreed(playerUUID, true);
                    plugin.hasAgree.put(player.getName(), false); // 设为 false 或从拦截列表中移除
                    player.sendMessage(MM.deserialize("<green>您已经同意用户协议，现在可以正常游玩！"));
                } else {
                    player.sendMessage(MM.deserialize("<yellow>你已经同意过用户协议！"));
                }
            });
        });

        return true;
    }
}