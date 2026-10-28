package work.alsace.buildercore.commands.home;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import work.alsace.buildercore.BuilderCore;
import work.alsace.buildercore.service.HomeDataLoader;

import java.util.Set;

public class HomesCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (work.alsace.buildercore.commands.CommandHelp.requested(sender, cmd, label, args)) return true;
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MM.deserialize("<red>该指令仅限玩家执行"));
            return true;
        }

        if (!player.hasPermission("buildercore.commands.homes")) {
            sender.sendMessage(MM.deserialize("<red>你没有使用该命令的权限"));
            return true;
        }

        HomeDataLoader loader = BuilderCore.instance.homeProfiles.get(player.getUniqueId());
        if (loader == null || loader.getHomes().isEmpty()) {
            player.sendMessage(MM.deserialize("<red>你没有任何传送点。"));
            return true;
        }

        loader.calculateMaxHomes();
        Set<String> homes = loader.getHomes();
        Component message = MM.deserialize("<gray>你的家 (" + homes.size() + "/" + (loader.getMaxHomes() == Integer.MAX_VALUE ? "∞" : loader.getMaxHomes()) + "): ");
        int count = 0;
        for (String home : homes) {
            message = message.append(Component.text(home, net.kyori.adventure.text.format.NamedTextColor.GOLD)
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/home " + home))
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("点击传送至 " + home))));
            if (++count < homes.size()) {
                message = message.append(Component.text(" | ", net.kyori.adventure.text.format.NamedTextColor.GRAY));
            }
        }
        player.sendMessage(message);
        return true;
    }
}