package work.alsace.buildercore.commands.home;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import work.alsace.buildercore.service.HomeTarget;

import java.util.Collections;
import java.util.List;

public class DelHomeCommand implements TabExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String USAGE = """
            <gray>正确指令:
            <white>/delhome <传送点></white> - 删除你的传送点
            <white>/delhome <玩家>:<传送点></white> - 删除指定玩家的传送点</gray>""";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sendMsg(sender, "<red>该指令仅限玩家执行");
            return true;
        }

        if (!player.hasPermission("buildercore.commands.delhome")) {
            sendMsg(player, "<red>你没有使用该命令的权限");
            return true;
        }

        if (args.length != 1) {
            sendMsg(player, USAGE);
            return true;
        }

        HomeTarget target = HomeTarget.parse(player, args[0], "buildercore.commands.delhome.other");
        if (target == null || target.loader() == null) {
            sendMsg(player, "<red>玩家不存在或无法获取数据");
            return true;
        }

        String homeName = target.homeName();
        var loader = target.loader();

        if (loader.getHome(homeName) == null) {
            sendMsg(player, "<red>不存在传送点 <white>" + MM.escapeTags(homeName) + "</white>");
            return true;
        }

        loader.delHome(homeName);
        sendMsg(player, "<gray>已成功删除传送点 <white>" + MM.escapeTags(homeName) + "</white>");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length != 1 || !(sender instanceof Player player)) return Collections.emptyList();

        HomeTarget target = HomeTarget.parse(player, args[0], "buildercore.commands.delhome.other");
        if (target != null && target.loader() != null) {
            String prefix = target.homeName().toLowerCase();
            return target.loader().getHomes().stream()
                    .filter(h -> h.startsWith(prefix))
                    .map(h -> target.isOther() ? target.ownerName() + ":" + h : h)
                    .toList();
        }
        return Collections.emptyList();
    }

    private void sendMsg(CommandSender sender, String text) {
        sender.sendMessage(MM.deserialize(text));
    }
}