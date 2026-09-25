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

    private static final List<String> commands = Arrays.asList("reload", "info");

    public BuilderCoreCommand(BuilderCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] args) {
        if (!sender.hasPermission("buildercore.admin")) {
            sender.sendMessage(MM.deserialize("<red>你没有使用该命令的权限"));
            return false;
        }
        if (args.length == 1) {
            switch (args[0]) {
                case "info" -> {
                    sender.sendMessage(MM.deserialize("<gray>插件名称: <white>AlsaceCore\n<gray>插件版本: <white>" + plugin.getDescription().getVersion() + "\n<gray>插件作者: <white>" + Arrays.toString(plugin.getDescription().getAuthors().toArray())));
                }
                case "reload" -> {
                    plugin.loadConfig();
                    sender.sendMessage(MM.deserialize("<gray>重载成功"));
                }
                default -> {
                    sender.sendMessage(MM.deserialize("<gray>正确指令:\n<white>/buildercore info <gray>- 查看插件信息\n<white>/buildercore reload <gray>- 重载插件"));
                }
            }
        } else {
            sender.sendMessage(MM.deserialize("<gray>正确指令:\n<white>/buildercore info <gray>- 查看插件信息\n<white>/buildercore reload <gray>- 重载插件"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String s, String[] args) {
        if (args.length == 1 && sender.hasPermission("buildercore.admin")) {
            return commands;
        }
        return new ArrayList<>(0);
    }
}
