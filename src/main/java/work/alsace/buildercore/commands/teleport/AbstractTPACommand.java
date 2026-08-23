package work.alsace.buildercore.commands.teleport;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import work.alsace.buildercore.BuilderCore;
import work.alsace.buildercore.service.TPAHandler;

import java.util.Collections;
import java.util.List;

public abstract class AbstractTPACommand implements TabExecutor {

    protected final BuilderCore plugin;
    protected final TPAHandler tpaHandler;
    protected static final MiniMessage MM = MiniMessage.miniMessage();

    private final boolean isTpaHere;
    private final String usageMessage;

    public AbstractTPACommand(BuilderCore plugin, boolean isTpaHere, String usageMessage) {
        this.plugin = plugin;
        this.tpaHandler = plugin.getTPAHandler();
        this.isTpaHere = isTpaHere;
        this.usageMessage = usageMessage;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length != 1 || !(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        String prefix = args[0].toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.equals(player))
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix))
                .toList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sendMsg(sender, "<red>该指令仅限玩家执行");
            return true;
        }

        if (!player.hasPermission("buildercore.commands.tpa")) {
            sendMsg(player, "<red>你没有使用该命令的权限");
            return true;
        }

        if (args.length != 1) {
            sendMsg(player, usageMessage);
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            sendMsg(player, "<red>玩家 " + args[0] + " 不在线或不存在。");
            return true;
        }

        if (target.equals(player)) {
            sendMsg(player, "<red>你不能向自己发送传送请求");
            return true;
        }

        tpaHandler.sendTPARequest(player, target, isTpaHere);
        return true;
    }

    protected void sendMsg(CommandSender sender, String miniMessageText) {
        sender.sendMessage(MM.deserialize(miniMessageText));
    }
}