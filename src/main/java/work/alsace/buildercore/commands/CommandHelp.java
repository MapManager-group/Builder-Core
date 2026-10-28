package work.alsace.buildercore.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.*;
import work.alsace.buildercore.BuilderCore;

import java.util.List;

public final class CommandHelp {
    private CommandHelp() {}

    public static boolean supportsFlag(String name) {
        return !java.util.Set.of("home", "delhome", "sethome", "warp", "delwarp", "setwarp", "itemname", "head",
                "/p1", "/p2", "/pa", "/s", "/r", "/f", "/c", "restart-vote").contains(name);
    }

    public static boolean requested(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1 || !args[0].equalsIgnoreCase("--help")
                || !supportsFlag(command == null ? label : command.getName())) return false;
        if (command != null && command.testPermissionSilent(sender)) show(sender, command, label);
        return true;
    }

    public static void show(CommandSender sender, Command command, String label) {
        sender.sendMessage(Component.text(command.getUsage().replace("<command>", label), NamedTextColor.WHITE)
                .append(Component.text("\n" + command.getDescription(), NamedTextColor.GRAY)));
        if (!command.getAliases().isEmpty())
            sender.sendMessage(Component.text("别名: " + String.join(", ", command.getAliases()), NamedTextColor.GRAY));
    }

    public static void list(BuilderCore plugin, CommandSender sender, String input) {
        if (!input.matches("[0-9]+")) {
            PluginCommand command = plugin.getCommand(input);
            if (command == null && input.startsWith("/")) command = plugin.getCommand(input.substring(1));
            if (command == null || !command.testPermissionSilent(sender)) {
                sender.sendMessage(Component.text("命令不存在或没有使用权限", NamedTextColor.RED));
            } else show(sender, command, command.getName());
            return;
        }
        List<PluginCommand> commands = plugin.getDescription().getCommands().keySet().stream()
                .map(plugin::getCommand).filter(command -> command.testPermissionSilent(sender))
                .filter(command -> command.getExecutor() != plugin).sorted(java.util.Comparator.comparing(Command::getName)).toList();
        int pages = Math.max(1, (commands.size() + 9) / 10);
        int page;
        try { page = Integer.parseInt(input); } catch (NumberFormatException exception) { page = 0; }
        if (page < 1 || page > pages) {
            sender.sendMessage(Component.text("页码范围: 1–" + pages, NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text("BuilderCore 命令帮助 " + page + "/" + pages, NamedTextColor.GOLD));
        commands.stream().skip((page - 1L) * 10).limit(10).forEach(command -> sender.sendMessage(
                Component.text("/" + command.getName() + " — " + command.getDescription(), NamedTextColor.GRAY)
                        .hoverEvent(HoverEvent.showText(Component.text(command.getUsage())))
                        .clickEvent(ClickEvent.suggestCommand("/" + command.getName() + " "))));
        sender.sendMessage(Component.text("/buildercore help <页码或命令>；固定参数命令也可用 --help 查看用法", NamedTextColor.GRAY));
    }
}
