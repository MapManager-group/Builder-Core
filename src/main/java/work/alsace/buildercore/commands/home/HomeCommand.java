package work.alsace.buildercore.commands.home;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import work.alsace.buildercore.BuilderCore;
import work.alsace.buildercore.service.HomeTarget;

import java.util.Collections;
import java.util.List;

public class HomeCommand implements TabExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String USAGE = """
            <gray>正确指令:
            <white>/home <传送点></white> - 传送至你的家
            <white>/home <玩家>:<传送点></white> - 传送至指定玩家的家</gray>""";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sendMsg(sender, "<red>该指令仅限玩家执行");
            return true;
        }

        if (!player.hasPermission("buildercore.commands.home")) {
            sendMsg(player, "<red>你没有使用该命令的权限");
            return true;
        }

        if (args.length != 1) {
            sendMsg(player, USAGE);
            return true;
        }

        HomeTarget target = HomeTarget.parse(player, args[0], "buildercore.commands.home.other");
        if (target == null || target.loader() == null) {
            sendMsg(player, "<red>无法获取指定玩家或传送点信息。");
            return true;
        }

        Location loc = target.loader().getHome(target.homeName());
        if (loc == null) {
            sendMsg(player, "<red>未找到传送点 <white>" + MM.escapeTags(target.homeName()) + "</white>");
            return true;
        }

        if (loc.getWorld() == null) {
            sendMsg(player, "<red>传送失败：目标世界不存在或未加载。");
            return true;
        }

        player.teleportAsync(loc);
        if (target.isOther()) {
            sendMsg(player, "<gray>已传送至玩家 <white>" + target.ownerName() + "</white> 的家 <white>" + MM.escapeTags(target.homeName()) + "</white>");
        } else {
            sendMsg(player, "<gray>已传送至家 <white>" + MM.escapeTags(target.homeName()) + "</white>");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length != 1 || !(sender instanceof Player player)) return Collections.emptyList();

        String input = args[0];
        if (input.contains(":") && player.hasPermission("buildercore.commands.home.other")) {
            String[] parts = input.split(":", 2);
            HomeTarget target = HomeTarget.parse(player, input, "buildercore.commands.home.other");
            if (target != null && target.loader() != null) {
                String prefix = parts[1].toLowerCase();
                return target.loader().getHomes().stream()
                        .filter(h -> h.startsWith(prefix))
                        .map(h -> parts[0] + ":" + h)
                        .toList();
            }
        } else {
            var loader = BuilderCore.instance.homeProfiles.get(player.getUniqueId());
            if (loader != null) {
                String prefix = input.toLowerCase();
                return loader.getHomes().stream()
                        .filter(h -> h.startsWith(prefix))
                        .toList();
            }
        }
        return Collections.emptyList();
    }

    private void sendMsg(CommandSender sender, String text) {
        sender.sendMessage(MM.deserialize(text));
    }
}