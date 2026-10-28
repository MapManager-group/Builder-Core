package work.alsace.buildercore.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import work.alsace.buildercore.BuilderCore;
import work.alsace.buildercore.service.HomeDataLoader;
import work.alsace.buildercore.service.WarpDataLoader;

import java.util.*;
import java.util.stream.Stream;

/** 所有注册命令共享权限、可见性和前缀过滤；补全不创建玩家数据。 */
public final class CommandCompletion implements TabCompleter {
    private final BuilderCore plugin;
    private final TabCompleter delegate;

    public CommandCompletion(BuilderCore plugin, TabCompleter delegate) {
        this.plugin = plugin;
        this.delegate = delegate;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || !command.testPermissionSilent(sender)) return List.of();
        List<String> candidates = new ArrayList<>(suggestions(sender, command, label, args));
        if (args.length == 1 && CommandHelp.supportsFlag(command.getName())) candidates.add("--help");
        String prefix = args[args.length - 1];
        return candidates.stream().filter(value -> value.regionMatches(true, 0, prefix, 0, prefix.length()))
                .filter(value -> visible(sender, value)).distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private List<String> suggestions(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName();
        if (!(sender instanceof Player) && !List.of("buildercore", "whois", "tp", "fly", "gamemode", "speed", "ptime").contains(name)) return List.of();
        if (name.equals("buildercore")) {
            if (args.length == 1) return List.of("help", "info", "reload");
            if (args.length == 2 && args[0].equalsIgnoreCase("help"))
                return plugin.getDescription().getCommands().keySet().stream()
                        .filter(key -> plugin.getCommand(key).testPermissionSilent(sender)).toList();
            return List.of();
        }
        if (name.equals("buildertools")) {
            if (args.length != 1 || !(sender instanceof Player)) return List.of();
            return Stream.of("ability", "armorcolor", "banner", "special", "rc")
                    .filter(mode -> sender.hasPermission(mode.equals("rc") ? "buildercore.commands.rc" : "buildercore.menu." + mode)).toList();
        }
        if (List.of("home", "delhome", "sethome").contains(name)) return homes(sender, name, args);
        if (List.of("warp", "delwarp", "setwarp").contains(name)) return warps(sender, name, args);
        if (name.equals("tp")) return teleport(sender, args);
        if (name.equals("head") || name.equals("whois") || name.equals("invsee")) return args.length == 1 ? players(sender) : List.of();
        if (name.equals("fly") || name.equals("endersee"))
            return args.length == 1 && sender.hasPermission("buildercore.commands." + name + ".other") ? players(sender) : List.of();
        if (name.equals("reach")) return args.length == 1 ? List.of("0", "4.5", "6", "16", "64") : List.of();
        if (name.equals("scale") || name.equals("/scale")) return args.length == 1 ? List.of("0.5", "1", "2") : List.of();
        if (name.equals("itemcolor")) return args.length == 1 ? List.of("#FFFFFF", "#000000", "#FF0000", "#00FF00", "#0000FF") : List.of();
        if (name.equals("itemname")) return args.length == 1 ? List.of("&f", "&a", "&b", "&e", "&l") : List.of();
        if (name.equals("/derot") || name.equals("/twist")) return switch (args.length) {
            case 1 -> List.of("x", "y", "z");
            case 2 -> List.of("45", "90", "180", "-90");
            default -> List.of();
        };
        String target = switch (name) {
            case "/p1" -> "/pos1"; case "/p2" -> "/pos2"; case "/pa" -> "/paste";
            case "/s" -> "/set"; case "/r" -> "/replace"; case "/f" -> "/flip"; case "/c" -> "/copy";
            default -> null;
        };
        if (target != null) {
            if (command instanceof PluginCommand registered && registered.getExecutor() == plugin) return List.of();
            Command forwarded = Bukkit.getServer().getCommandMap().getCommand(target);
            return forwarded == null || !forwarded.testPermissionSilent(sender) ? List.of() : forwarded.tabComplete(sender, target, args);
        }
        List<String> result = delegate == null ? List.of() : delegate.onTabComplete(sender, command, label, args);
        return result == null ? List.of() : result;
    }

    private List<String> homes(CommandSender sender, String command, String[] args) {
        if (!(sender instanceof Player player) || args.length != 1) return List.of();
        String input = args[0];
        int colon = input.indexOf(':');
        boolean other = sender.hasPermission("buildercore.commands." + command + ".other");
        if (colon >= 0 && other) {
            String owner = input.substring(0, colon);
            var target = Bukkit.getOfflinePlayerIfCached(owner);
            if (target == null) return List.of();
            HomeDataLoader loader = plugin.homeProfiles.get(target.getUniqueId());
            if (loader == null && new java.io.File(plugin.getDataFolder(), "userdata/" + target.getUniqueId() + ".yml").isFile()) {
                try { loader = new HomeDataLoader(target.getUniqueId()); }
                catch (IllegalStateException exception) { return List.of(); }
            }
            return loader == null ? List.of() : loader.getHomes().stream().map(home -> owner + ":" + home).toList();
        }
        HomeDataLoader loader = plugin.homeProfiles.get(player.getUniqueId());
        List<String> result = new ArrayList<>(loader == null ? Set.of() : loader.getHomes());
        if (other) players(sender).forEach(name -> result.add(name + ":"));
        return result;
    }

    private List<String> warps(CommandSender sender, String command, String[] args) {
        if (!(sender instanceof Player) || args.length != 1 || command.equals("setwarp")) return List.of();
        WarpDataLoader loader = plugin.warpProfiles.get("warps");
        List<String> names = new ArrayList<>();
        for (String name : loader.getWarps()) {
            if (command.equals("warp") && loader.getWarpWorld(name) == null) continue;
            names.add(name);
            String alias = loader.getWarpAlias(name);
            if (alias != null) names.add(alias);
        }
        return names;
    }

    private List<String> teleport(CommandSender sender, String[] args) {
        boolean location = sender.hasPermission("buildercore.commands.tp.location");
        boolean player = sender.hasPermission("buildercore.commands.tp.player");
        boolean other = sender.hasPermission("buildercore.commands.tp.other");
        List<String> result = new ArrayList<>();
        if (args.length == 1 && player && sender instanceof Player moving) {
            players(sender).stream().filter(name -> work.alsace.buildercore.service.TeleportAccess.canTeleport(plugin, moving, Bukkit.getPlayerExact(name).getWorld(), true)).forEach(result::add);
        } else if (args.length == 1 && other && (player || location)) result.addAll(players(sender));
        if (args.length == 2 && player && other) {
            Player moving = Bukkit.getPlayer(args[0]);
            if (moving != null) players(sender).stream()
                    .filter(name -> work.alsace.buildercore.service.TeleportAccess.canTeleport(plugin, moving, Bukkit.getPlayerExact(name).getWorld(), true)).forEach(result::add);
        }
        if (location && sender instanceof Player && (args.length == 1 || args.length <= 3 && coordinate(args[0]))) result.add("~");
        if (location && args.length >= 2 && args.length <= 4 && (other || sender instanceof Player p && (args[0].equalsIgnoreCase(p.getName()) || args[0].equalsIgnoreCase("@s")))) result.add("~");
        if (location && sender instanceof Player && args.length == 1) result.add("@s");
        return result;
    }

    private boolean coordinate(String input) {
        if (input.startsWith("~")) return true;
        try { return Double.isFinite(Double.parseDouble(input)); }
        catch (NumberFormatException exception) { return false; }
    }

    private List<String> players(CommandSender sender) {
        return Bukkit.getOnlinePlayers().stream().filter(player -> visible(sender, player.getName())).map(Player::getName).toList();
    }

    private boolean visible(CommandSender sender, String name) {
        Player target = Bukkit.getPlayerExact(name);
        return !(sender instanceof Player player) || target == null || player.canSee(target);
    }
}
