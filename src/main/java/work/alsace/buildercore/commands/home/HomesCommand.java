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

        Set<String> homes = loader.getHomes();
        StringBuilder sb = new StringBuilder("<gray>你的家 (" + homes.size() + "/" + (loader.getMaxHomes() == Integer.MAX_VALUE ? "∞" : loader.getMaxHomes()) + "): ");

        int count = 0;
        for (String home : homes) {
            sb.append("<gold><click:run_command:'/home ").append(home)
                    .append("'><hover:show_text:'<gray>点击传送至 <white>").append(home).append("</white></gray>'>")
                    .append(home)
                    .append("</hover></click></gold>");

            if (++count < homes.size()) {
                sb.append("<gray> | </gray>");
            }
        }

        player.sendMessage(MM.deserialize(sb.toString()));
        return true;
    }
}