package work.alsace.buildercore.commands.teleport;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class TPHereCommand implements TabExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String USAGE = "<gray>正确指令:\n<white>/tphere <玩家></white> - 将指定玩家传送到你的位置</gray>";

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length != 1 || !(sender instanceof Player)) {
            return Collections.emptyList();
        }

        String prefix = args[0].toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix))
                .toList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player commander)) {
            sendMsg(sender, "<red>该指令仅限玩家执行");
            return true;
        }

        if (args.length != 1) {
            sendMsg(commander, USAGE);
            return true;
        }

        if (!commander.hasPermission("buildercore.commands.tphere")) {
            sendMsg(commander, "<red>你没有使用该命令的权限");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sendMsg(commander, "<red>指定的玩家不在线或不存在");
            return true;
        }

        if (target.equals(commander)) {
            sendMsg(commander, "<red>你不能将自己传送给自己");
            return true;
        }

        // 异步传送
        target.teleportAsync(commander.getLocation()).thenAccept(success -> {
            if (success) {
                sendMsg(target, "<white>" + commander.getName() + "</white> <gray>将你传送到了他的位置");
                sendMsg(commander, "<gray>已将 <white>" + target.getName() + "</white> 传送到你的位置");
            } else {
                sendMsg(commander, "<red>传送失败，请稍后再试");
            }
        });

        return true;
    }

    private void sendMsg(CommandSender sender, String miniMessageText) {
        sender.sendMessage(MM.deserialize(miniMessageText));
    }
}