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

public class DisagreeCommand implements CommandExecutor {
    private final BuilderCore plugin;
    private final DataBaseManager databaseManager;

    public DisagreeCommand(BuilderCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, String[] args) {
        if (sender instanceof Player player) {
            UUID playerUUID = player.getUniqueId();
            databaseManager.delPlayerAgreed(playerUUID, false);
            player.kick(Component.text("您必须阅读并同意《阿尔萨斯工业园用户协议》才能使用工业园的服务！", NamedTextColor.RED));
            return true;

        } else {
            sender.sendMessage(Component.text("只有玩家可以使用这个命令！", NamedTextColor.RED));
            return false;
        }
    }

}
