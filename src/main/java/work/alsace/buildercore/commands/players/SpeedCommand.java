package work.alsace.buildercore.commands.players;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SpeedCommand implements TabExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Component error = MM.deserialize("<gray>正确指令:\n<white>/speed <速度> <gray>- 设置你的飞行或步行速度(0.1~10)\n<white>/speed <类型> <速度> [玩家] <gray>- 设置指定玩家的指定类型速度");
    private static final List<String> types = Arrays.asList("walk", "fly", "1", "1.5", "1.75", "2");
    private static final List<String> speeds = Arrays.asList("1", "1.5", "1.75", "2");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (work.alsace.buildercore.commands.CommandHelp.requested(sender, command, s, strings)) return true;
        SpeedChange change = parseChange(sender, strings);
        if (change == null) return false;
        Player target = change.player();
        if (change.flying()) target.setFlySpeed(change.speed() / 10F);
        else target.setWalkSpeed(change.speed() / 10F);
        String type = change.flying() ? "飞行" : "步行";
        if (strings.length == 3) sender.sendMessage(MM.deserialize("<gray>已将玩家 <white>" + target.getName() + " <gray>的 <white>" + type + " <gray>速度设置为 <white>" + change.speed()));
        target.sendMessage(MM.deserialize("<gray>已将你的 <white>" + type + " <gray>速度设置为 <white>" + change.speed()));
        return true;
    }

    private record SpeedChange(Player player, boolean flying, float speed) { }

    private SpeedChange parseChange(CommandSender sender, String[] args) {
        Player target = target(sender, args);
        if (target == null) return null;
        try {
            boolean fly = args.length == 1 ? target.isFlying() : switch (args[0]) {
                case "walk" -> false;
                case "fly" -> true;
                default -> throw new IllegalArgumentException();
            };
            float speed = Float.parseFloat(args[args.length == 1 ? 0 : 1]);
            if (!Float.isFinite(speed) || speed < 0.1F || speed > 10) throw new IllegalArgumentException();
            return new SpeedChange(target, fly, speed);
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(error);
            return null;
        }
    }

    private Player target(CommandSender sender, String[] args) {
        if (args.length < 1 || args.length > 3 || args.length < 3 && !(sender instanceof Player)) {
            sender.sendMessage(error);
            return null;
        }
        if (!sender.hasPermission(args.length == 3 ? "buildercore.commands.speed.other" : "buildercore.commands.speed")) {
            sender.sendMessage(MM.deserialize("<red>你没有使用该命令的权限"));
            return null;
        }
        Player target = args.length == 3 ? Bukkit.getPlayer(args[2]) : (Player) sender;
        if (target == null) sender.sendMessage(MM.deserialize("<red>指定的玩家不在线或不存在"));
        return target;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] strings) {
        if (strings.length == 1 && sender.hasPermission("buildercore.commands.speed")) {
            return types;
        }
        if (strings.length == 2 && sender.hasPermission("buildercore.commands.speed")) {
            return speeds;
        }
        if (strings.length == 3 && sender.hasPermission("buildercore.commands.speed.other")) {
            List<String> list = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!(sender instanceof Player viewer) || viewer.canSee(p)) list.add(p.getName());
            }
            return list;
        }
        return Collections.emptyList();
    }
}
