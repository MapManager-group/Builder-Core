package work.alsace.buildercore.commands.teleport;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import work.alsace.buildercore.BuilderCore;

import java.util.Collections;
import java.util.List;

public class TPIgnoreCommand implements TabExecutor {

    private final BuilderCore plugin;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public TPIgnoreCommand(BuilderCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (work.alsace.buildercore.commands.CommandHelp.requested(sender, cmd, label, args)) return true;
        if (!(sender instanceof Player player)) {
            sendMsg(sender, "<red>该指令仅限玩家使用");
            return true;
        }

        if (!player.hasPermission("buildercore.commands.tpignore")) {
            sendMsg(player, "<red>你没有使用该命令的权限");
            return true;
        }

        boolean newState = this.plugin.hasIgnored.compute(player.getName(), (k, v) -> (v == null) ? Boolean.TRUE : !v);

        if (newState) {
            sendMsg(player, "<gray>已成功屏蔽强制传送");
        } else {
            sendMsg(player, "<gray>已取消屏蔽强制传送");
        }

        return true;
    }

    private void sendMsg(CommandSender sender, String miniMessageText) {
        sender.sendMessage(MM.deserialize(miniMessageText));
    }
}