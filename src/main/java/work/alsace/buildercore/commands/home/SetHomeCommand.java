package work.alsace.buildercore.commands.home;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import work.alsace.buildercore.BuilderCore;
import work.alsace.buildercore.service.HomeTarget;

import java.util.regex.Pattern;

public class SetHomeCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String USAGE = """
            <gray>正确指令:
            <white>/sethome <传送点></white> - 设置你的家
            <white>/sethome <玩家>:<传送点></white> - 设置指定玩家的家</gray>""";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sendMsg(sender, "<red>该指令仅限玩家执行");
            return true;
        }

        if (!player.hasPermission("buildercore.commands.sethome")) {
            sendMsg(player, "<red>你没有使用该命令的权限");
            return true;
        }

        if (args.length != 1) {
            sendMsg(player, USAGE);
            return true;
        }

        HomeTarget target = HomeTarget.parse(player, args[0], "buildercore.commands.sethome.other");
        if (target == null || target.loader() == null) {
            sendMsg(player, "<red>玩家不存在或无法获取数据");
            return true;
        }

        String homeName = target.homeName();
        var loader = target.loader();

        if (loader.getHome(homeName) != null) {
            sendMsg(player, "<red>已经存在名为 <white>" + homeName + "</white> 的传送点");
            return true;
        }

        if (!target.isOther() && loader.getHomes().size() >= loader.getMaxHomes()) {
            sendMsg(player, "<red>你设置的家数量已达上限 (" + loader.getMaxHomes() + ")");
            return true;
        }

        // 非法字符检查
        String illegalRegex = BuilderCore.instance.getConfig().getString("illegal-characters");
        if (illegalRegex != null && !illegalRegex.isEmpty()) {
            Pattern pattern = Pattern.compile(illegalRegex);
            if (pattern.matcher(homeName).find()) {
                sendMsg(player, "<red>传送点名称包含非法字符");
                return true;
            }
        }

        loader.addHome(homeName, player.getLocation());
        sendMsg(player, "<gray>成功设置传送点 <white>" + homeName + "</white>");
        return true;
    }

    private void sendMsg(CommandSender sender, String text) {
        sender.sendMessage(MM.deserialize(text));
    }
}