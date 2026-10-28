package work.alsace.buildercore.commands.tools;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.*;
import work.alsace.buildercore.BuilderCore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BuilderCoreCommand implements TabExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final BuilderCore plugin;

    private static final List<String> commands = Arrays.asList("reload", "info", "help");

    public BuilderCoreCommand(BuilderCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] args) {
        if (work.alsace.buildercore.commands.CommandHelp.requested(sender, cmd, s, args)) return true;
        if (!sender.hasPermission("buildercore.admin")) {
            sender.sendMessage(MM.deserialize("<red>你没有使用该命令的权限"));
            return false;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            work.alsace.buildercore.commands.CommandHelp.list(plugin, sender, args.length < 2 ? "1" : args[1]);
            return true;
        }
        if (args.length == 1) {
            switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
                case "info" -> {
                    sender.sendMessage(MM.deserialize("<gray>插件名称: <white>" + plugin.getDescription().getName() + "\n<gray>插件版本: <white>" + plugin.getDescription().getVersion() + "\n<gray>插件作者: <white>" + Arrays.toString(plugin.getDescription().getAuthors().toArray())));
                }
                case "reload" -> {
                    reload(sender);
                }
                default -> {
                    work.alsace.buildercore.commands.CommandHelp.show(sender, cmd, s);
                }
            }
        } else {
            work.alsace.buildercore.commands.CommandHelp.show(sender, cmd, s);
        }
        return true;
    }

    private void reload(CommandSender sender) {
        try {
            plugin.loadConfig();
            sender.sendMessage(MM.deserialize("<gray>重载成功"));
        } catch (IllegalStateException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "配置或位置数据重载失败", exception);
            sender.sendMessage(MM.deserialize("<red>重载失败，原数据文件已保留，请查看服务端日志。"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String s, String[] args) {
        if (args.length == 1 && sender.hasPermission("buildercore.admin")) {
            return commands;
        }
        return new ArrayList<>(0);
    }
}
