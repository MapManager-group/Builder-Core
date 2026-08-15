package work.alsace.buildercore.commands.agree;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

    public AgreeCommand(BuilderCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, String[] args) {
        if (sender instanceof Player player) {
            UUID playerUUID = player.getUniqueId();

            if (!databaseManager.hasPlayerAgreed(playerUUID)) {
                databaseManager.setPlayerAgreed(playerUUID, true);
                plugin.hasAgree.put(player.getName(), false);
                player.sendMessage(Component.text("您已经同意用户协议，现在可以正常游玩！", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("你已经同意过用户协议！", NamedTextColor.GREEN));
                return false;
            }
        } else {
            sender.sendMessage(Component.text("只有玩家可以使用这个命令！", NamedTextColor.RED));
            return false;
        }
        return true;
    }
}

