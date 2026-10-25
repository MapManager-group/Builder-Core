package work.alsace.buildercore.service;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import work.alsace.buildercore.BuilderCore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TPAHandler {

    private final BuilderCore plugin;
    private final Map<UUID, TPARequest> tpaRequests = new ConcurrentHashMap<>();
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final long REQUEST_TIMEOUT_MS = 60 * 1000L; // 60 秒超时

    public TPAHandler(BuilderCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 发送传送请求
     *
     * @param sender    发送请求的玩家
     * @param target    接收请求的玩家
     * @param isTpaHere 是否是 TPAHere (true 表示邀请目标到自己这里，false 表示自己传送过去)
     */
    public void sendTPARequest(Player sender, Player target, boolean isTpaHere) {
        Player moving = isTpaHere ? target : sender;
        Player destination = isTpaHere ? sender : target;
        if (!TeleportAccess.canTeleport(plugin, moving, destination.getWorld(), false)) {
            sendMsg(sender, "<red>传送玩家没有目标世界的进入权限，无法发送请求。");
            return;
        }
        tpaRequests.put(target.getUniqueId(), new TPARequest(sender.getUniqueId(), isTpaHere));

        if (isTpaHere) {
            sendMsg(sender, "<gray>已向 <aqua>" + target.getName() + "</aqua> 发送了传送邀请，邀请其传送到你这里。</gray>");
            sendMsg(target, "<aqua>" + sender.getName() + "</aqua> <gray>邀请你传送到他/她的位置。</gray>");
        } else {
            sendMsg(sender, "<gray>已向 <aqua>" + target.getName() + "</aqua> 发送了传送请求。</gray>");
            sendMsg(target, "<aqua>" + sender.getName() + "</aqua> <gray>请求传送到你的位置。</gray>");
        }

        // MiniMessage 交互式点击按钮：点击聊天框直接触发指令
        String interactivePrompt = """
                <gray>请在 60 秒内响应：\
                <green><bold><click:run_command:'/tpaccept'>[接受]</click></bold></green> \
                <red><bold><click:run_command:'/tpdeny'>[拒绝]</click></bold></red></gray>""";
        sendMsg(target, interactivePrompt);
    }

    public void acceptTPA(Player target) {
        UUID targetId = target.getUniqueId();
        TPARequest request = tpaRequests.remove(targetId);

        if (request == null) {
            sendMsg(target, "<red>你没有待处理的传送请求。");
            return;
        }

        if (request.isExpired()) {
            sendMsg(target, "<red>该传送请求已过期。");
            return;
        }

        Player sender = Bukkit.getPlayer(request.senderId());
        if (sender == null || !sender.isOnline()) {
            sendMsg(target, "<red>发送请求的玩家已离线。");
            return;
        }

        Player moving = request.isTpaHere() ? target : sender;
        Player destination = request.isTpaHere() ? sender : target;
        if (!sender.hasPermission("buildercore.commands.tpa")
                || !TeleportAccess.canTeleport(plugin, moving, destination.getWorld(), false)) {
            sendMsg(target, "<red>传送权限已失效，无法接受请求。");
            sendMsg(sender, "<red>传送权限已失效，请重新确认命令及目标世界的进入权限。");
            return;
        }

        // 根据请求类型执行异步传送
        if (request.isTpaHere()) {
            sendMsg(target, "<green>你已接受请求，正在传送至 <white>" + sender.getName() + "</white> 身边...");
            sendMsg(sender, "<green><white>" + target.getName() + "</white> 接受了你的邀请，正在传送...");

            target.teleportAsync(sender.getLocation());
        } else {
            sendMsg(target, "<green>你接受了 <white>" + sender.getName() + "</white> 的传送请求。");
            sendMsg(sender, "<green><white>" + target.getName() + "</white> 接受了你的传送请求，正在传送...");

            sender.teleportAsync(target.getLocation());
        }
    }

    public void denyTPA(Player target) {
        UUID targetId = target.getUniqueId();
        TPARequest request = tpaRequests.remove(targetId);

        if (request == null) {
            sendMsg(target, "<red>你没有待处理的传送请求。");
            return;
        }

        sendMsg(target, "<gray>你拒绝了传送请求。");

        Player sender = Bukkit.getPlayer(request.senderId());
        if (sender != null && sender.isOnline()) {
            sendMsg(sender, "<red>" + target.getName() + " 拒绝了你的传送请求。");
        }
    }

    private void sendMsg(Player player, String miniMessageText) {
        player.sendMessage(MM.deserialize(miniMessageText));
    }

    private record TPARequest(UUID senderId, boolean isTpaHere, long timestamp) {
        public TPARequest(UUID senderId, boolean isTpaHere) {
            this(senderId, isTpaHere, System.currentTimeMillis());
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > REQUEST_TIMEOUT_MS;
        }
    }
}