package work.alsace.buildercore.commands.tools;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import work.alsace.buildercore.BuilderCore;

import java.util.ArrayList;
import java.util.List;

public class RestartVoteCommand implements CommandExecutor, TabCompleter, Listener {

    //没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完
    //没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完
    //没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完
    //没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完没写完

    private final BuilderCore plugin;
    public RestartVoteCommand(BuilderCore plugin) {
        this.plugin = plugin;
    }

    private boolean inVote;
    private int yesVote;
    private BukkitTask countdownTask;

    private List<String> players = new ArrayList<>();

    private final String error = ChatColor.GRAY + "正确指令:\n§f/restart-vote <理由> §7- 发起重启投票\n§f/restart-vote #yes §7- 同意投票\n§f/restart-vote #no §7- 拒绝投票";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "控制台无法执行该命令");
            return false;
        }

        if (player.hasPermission("buildercore.commands.vote")) {
            player.sendMessage(ChatColor.RED + "你没有使用该命令的权限");
            return false;
        }

        if (args.length < 1) {
            player.sendMessage(error);
            return false;
        }

        //同意
        if (args[0].equalsIgnoreCase("#yes") && inVote) {
            if (!players.contains(player.getName())) {
                yesVote ++;
                players.remove(player.getName());
                player.sendMessage(ChatColor.GREEN + "投票成功，当前同意数量：" + yesVote);
            } else {
                player.sendMessage(ChatColor.YELLOW + "你不在投票玩家池内或已经投票过了。");
            }
            return true;
        }

        //拒绝
        if  (args[0].equalsIgnoreCase("#no") && inVote) {
            CancelVote(player.getName());
            return true;
        }

        if (inVote) {
            player.sendMessage(ChatColor.YELLOW + "已经存在一个投票了，请等待当前投票结束。");
        } else {
            RunVote(args[0]);
        }

        return true;
    }

    //运行
    public void RunVote(String reason) {
        inVote = true;
        yesVote = 0;
        players.clear();

        for (Player player : Bukkit.getOnlinePlayers()) {
            players.add(player.getName());
        }

        countdownTask = new BukkitRunnable() {
            int secondsLeft = 180;
            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    CheckVote();
                    cancel();
                } else {
                    secondsLeft--;
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    //检查
    public void CheckVote() {
        inVote = false;
        if (yesVote >= players.size() * 0.8) {

            Bukkit.broadcastMessage(ChatColor.GREEN + "[投票重启] 投票通过，服务器将在两分钟后重启!");

            BukkitTask stopServer = new BukkitRunnable() {
                int secondsLeft = 120;
                @Override
                public void run() {
                    if (secondsLeft <= 0) {
                        Bukkit.shutdown();
                    } else {
                        if (secondsLeft <= 20) {
                            Bukkit.broadcastMessage(ChatColor.DARK_RED + "[投票重启] 服务器将在 " + secondsLeft + " 秒后重启!");
                        }
                        secondsLeft--;
                    }
                }
            }.runTaskTimer(plugin, 0L, 20L);

            return;
        }

        Bukkit.broadcastMessage(ChatColor.RED + "[投票重启] 投票同意玩家未达到全部玩家的80%，本次重启已取消。");
    }

    //取消
    public void CancelVote(String playerName) {
        Bukkit.broadcastMessage(ChatColor.RED + "[投票重启] 玩家 " + playerName + " 拒绝了重启投票，本次投票已自动终止。");
        if (countdownTask != null) {
            countdownTask.cancel();
            inVote = false;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return List.of();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        players.remove(event.getPlayer().getName());
    }
}
