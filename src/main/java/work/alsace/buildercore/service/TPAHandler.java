package work.alsace.buildercore.service;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import work.alsace.buildercore.BuilderCore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TPAHandler {
    private final BuilderCore plugin;
    // 使用目标玩家的 UUID 作为 Key，存放他们收到的传送请求
    private final Map<UUID, TPARequest> tpaRequests;
    // 请求过期时间 (毫秒)，这里设置为 60 秒
    private final long REQUEST_TIMEOUT = 60 * 1000L;

    public TPAHandler(BuilderCore plugin) {
        this.plugin = plugin;
        this.tpaRequests = new HashMap<>();
    }

    /**
     * 发送传送请求
     * @param sender 发送请求的玩家
     * @param target 接收请求的玩家
     * @param isTpaHere 是否是 TPAHere (true 表示拉人，false 表示传送到目标)
     */
    public void sendTPARequest(Player sender, Player target, boolean isTpaHere) {
        tpaRequests.put(target.getUniqueId(), new TPARequest(sender.getUniqueId(), isTpaHere));

        if (isTpaHere) {
            sender.sendMessage(ChatColor.GRAY + "已向 " + ChatColor.AQUA + target.getName() + ChatColor.GRAY + " 发送了请求，邀请其传送到你这里。");
            target.sendMessage(ChatColor.AQUA + sender.getName() + ChatColor.GRAY + " 想让你传送到他(她)的位置。");
        } else {
            sender.sendMessage(ChatColor.GRAY + "已向 " + ChatColor.AQUA + target.getName() + ChatColor.GRAY + " 发送了传送请求。");
            target.sendMessage(ChatColor.AQUA + sender.getName() + ChatColor.GRAY + " 想要传送到你这里。");
        }

        target.sendMessage(ChatColor.GRAY + "输入 " + ChatColor.GREEN + "/tpaccept" + ChatColor.GRAY + " 接受，或者 " + ChatColor.RED + "/tpdeny" + ChatColor.GRAY + " 拒绝 (60秒内有效)。");
    }

    public void acceptTPA(Player target) {
        UUID targetId = target.getUniqueId();

        if (!tpaRequests.containsKey(targetId)) {
            target.sendMessage(ChatColor.RED + "你没有待处理的传送请求。");
            return;
        }

        TPARequest request = tpaRequests.get(targetId);
        tpaRequests.remove(targetId); // 无论接受成功与否，处理后都移除请求

        // 检查请求是否过期
        if (System.currentTimeMillis() - request.getTimestamp() > REQUEST_TIMEOUT) {
            target.sendMessage(ChatColor.RED + "该传送请求已过期。");
            return;
        }

        Player sender = Bukkit.getPlayer(request.getSenderId());

        if (sender == null || !sender.isOnline()) {
            target.sendMessage(ChatColor.RED + "发送请求的玩家已离线。");
            return;
        }

        // 根据请求类型执行传送
        if (request.isTpaHere()) {
            target.teleport(sender.getLocation());
            target.sendMessage(ChatColor.GREEN + "你已接受请求，正在传送到 " + sender.getName() + " 的位置。");
            sender.sendMessage(ChatColor.GREEN + target.getName() + " 接受了你的请求，已传送到你身边。");
        } else {
            sender.teleport(target.getLocation());
            target.sendMessage(ChatColor.GREEN + "你接受了 " + sender.getName() + " 的传送请求。");
            sender.sendMessage(ChatColor.GREEN + target.getName() + " 接受了你的传送请求，正在传送...");
        }
    }

    public void denyTPA(Player target) {
        UUID targetId = target.getUniqueId();

        if (!tpaRequests.containsKey(targetId)) {
            target.sendMessage(ChatColor.RED + "你没有待处理的传送请求。");
            return;
        }

        TPARequest request = tpaRequests.remove(targetId);
        target.sendMessage(ChatColor.GRAY + "你拒绝了传送请求。");

        // 如果发送者还在线，通知他被拒绝了
        Player sender = Bukkit.getPlayer(request.getSenderId());
        if (sender != null && sender.isOnline()) {
            sender.sendMessage(ChatColor.RED + target.getName() + " 拒绝了你的传送请求。");
        }
    }

    /**
     * 内部类：用于封装和记录传送请求的信息
     */
    private static class TPARequest {
        private final UUID senderId;
        private final boolean isTpaHere;
        private final long timestamp;

        public TPARequest(UUID senderId, boolean isTpaHere) {
            this.senderId = senderId;
            this.isTpaHere = isTpaHere;
            this.timestamp = System.currentTimeMillis();
        }

        public UUID getSenderId() {
            return senderId;
        }

        public boolean isTpaHere() {
            return isTpaHere;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}