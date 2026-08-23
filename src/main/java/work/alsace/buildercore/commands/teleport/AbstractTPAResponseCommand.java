package work.alsace.buildercore.commands.teleport;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import work.alsace.buildercore.BuilderCore;
import work.alsace.buildercore.service.TPAHandler;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractTPAResponseCommand implements TabExecutor {

    protected final TPAHandler tpaHandler;
    private final Consumer<Player> action;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public AbstractTPAResponseCommand(BuilderCore plugin, Consumer<Player> action) {
        this.tpaHandler = plugin.getTPAHandler();
        this.action = action;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MM.deserialize("<red>该指令仅限玩家执行"));
            return true;
        }

        if (!player.hasPermission("buildercore.commands.tpa")) {
            player.sendMessage(MM.deserialize("<red>你没有使用该命令的权限"));
            return true;
        }

        action.accept(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        // 无需额外参数补全，返回空列表
        return Collections.emptyList();
    }
}