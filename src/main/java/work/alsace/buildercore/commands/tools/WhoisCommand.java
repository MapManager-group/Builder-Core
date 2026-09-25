package work.alsace.buildercore.commands.tools;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WhoisCommand implements CommandExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Component error = MM.deserialize("<gray>正确指令:\n<white>/whois <玩家> <gray>- 查询指定玩家信息");
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (strings.length == 1 && sender.hasPermission("buildercore.commands.whois")) {
            Player player = sender.getServer().getPlayer(strings[0]);
            if (player == null) {
                sender.sendMessage(MM.deserialize("<red>指定的玩家不在线或不存在"));
                return false;
            }

            Location location = player.getLocation();
            Component uuidMessage = MM.deserialize("<gray>UUID: <green>" + player.getUniqueId())
                    .clickEvent(ClickEvent.copyToClipboard(player.getUniqueId().toString()))
                    .hoverEvent(HoverEvent.showText(Component.text("点击复制")));

            sender.sendMessage(MM.deserialize("<gray>用户名: <white>" + player.getName()));
            sender.sendMessage(uuidMessage);
            sender.sendMessage(MM.deserialize("<gray>IP地址: <white>" + player.getAddress().getHostString()));
            sender.sendMessage(MM.deserialize("<gray>OP权限: <white>" + player.isOp()));
            sender.sendMessage(MM.deserialize("<gray>游戏模式: <white>" + player.getGameMode()));
            sender.sendMessage(MM.deserialize("<gray>坐标位置: <white>X:" + Math.floor(location.getX()) + " Y:" + Math.floor(location.getY()) + " Z:" + Math.floor(location.getZ())));
            sender.sendMessage(MM.deserialize("<gray>所在世界: <white>" + player.getWorld().getName()));
            return true;
        }
        sender.sendMessage(error);
        return false;
    }
}
