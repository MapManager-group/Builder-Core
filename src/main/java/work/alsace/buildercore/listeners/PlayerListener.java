package work.alsace.buildercore.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import work.alsace.buildercore.BuilderCore;
import work.alsace.buildercore.commands.builderTools.AdvanceFlyCommand;
import work.alsace.buildercore.commands.teleport.BackCommand;
import work.alsace.buildercore.service.HomeDataLoader;
import work.alsace.buildercore.utils.DataBaseManager;
import work.alsace.buildercore.utils.NoClipUtil;

import java.util.Objects;
import java.util.UUID;

public class PlayerListener implements Listener {

    private final BuilderCore plugin;
    private final DataBaseManager databaseManager;

    public PlayerListener(BuilderCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.joinMessage(Component.empty());
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        BlockListener.slabs.add(player);
        plugin.homeProfiles.put(playerUUID, new HomeDataLoader(playerUUID));

        if (plugin.agreement && databaseManager != null) {
            databaseManager.hasPlayerAgreed(playerUUID).thenAccept(hasAgreed -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;

                    if (!hasAgreed) {
                        // 标记为未同意
                        plugin.hasAgree.put(player.getName(), true);

                        if (!player.hasPlayedBefore()) {
                            Component welcomeMsg = Component.text("欢迎新玩家 ", NamedTextColor.GREEN)
                                    .append(Component.text(player.getName(), NamedTextColor.WHITE))
                                    .append(Component.text(" 加入服务器！", NamedTextColor.GREEN));
                            Bukkit.broadcast(welcomeMsg);
                        }

                        sendAgreementMessage(player);
                    } else {
                        // 标记为已同意
                        plugin.hasAgree.put(player.getName(), false);
                    }
                });
            });
        } else {
            plugin.hasAgree.put(player.getName(), false);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.quitMessage(Component.empty());
        Player player = event.getPlayer();

        plugin.hasIgnored.remove(player.getName());
        plugin.hasAgree.remove(player.getName());
        plugin.homeProfiles.remove(player.getUniqueId());

        NoClipUtil.noclip.remove(player);
        AdvanceFlyCommand.enabledPlayers.remove(player);
        BlockListener.slabs.remove(player);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        // 直接读取内存中的 hasAgree 标志位，效率极高且不涉及数据库
        if (plugin.agreement && Boolean.TRUE.equals(plugin.hasAgree.get(player.getName()))) {
            // 只在位置变化（XYZ）时取消，允许玩家旋转视角
            if (event.getFrom().getX() != event.getTo().getX() ||
                    event.getFrom().getY() != event.getTo().getY() ||
                    event.getFrom().getZ() != event.getTo().getZ()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (plugin.agreement && Boolean.TRUE.equals(plugin.hasAgree.get(player.getName()))) {
            String message = event.getMessage().toLowerCase();
            if (!message.startsWith("/agree") && !message.startsWith("/disagree")) {
                event.setCancelled(true);
                player.sendMessage(Component.text("您必须先同意用户协议才能使用其他命令！请输入 /agree 进行同意。", NamedTextColor.RED));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.agreement && Boolean.TRUE.equals(plugin.hasAgree.get(player.getName()))) {
            event.setCancelled(true);
            player.sendMessage(Component.text("您必须先阅读并同意用户协议才能聊天！请输入 /agree 进行同意。", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause().equals(TeleportCause.SPECTATE) && !event.getPlayer().hasPermission("buildercore.event.spectate")) {
            event.setCancelled(true);
        }

        // 记录传送历史位置以支持 /back
        if (plugin.getCommand("back") != null && plugin.getCommand("back").getExecutor() instanceof BackCommand backCommand) {
            backCommand.addToHistory(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        plugin.homeProfiles.remove(event.getPlayer().getUniqueId());
        plugin.hasAgree.remove(event.getPlayer().getName());
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack item = event.getResult();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            // 使用 Adventure LegacySerializer 解析包含 & 色彩代码和 Hex 的字符串
            String rawName = LegacyComponentSerializer.legacyAmpersand().serialize(meta.displayName());
            Component formattedName = LegacyComponentSerializer.legacyAmpersand().deserialize(rawName);

            meta.displayName(formattedName);
            item.setItemMeta(meta);
            event.setResult(item);
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    private void sendAgreementMessage(Player player) {
        Component welcomeComponent = Component.text("您必须阅读并同意 ", NamedTextColor.GOLD);
        Component agreementLink = Component.text("《" + plugin.getUrlTitle() + "》", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.openUrl(plugin.getUrl()));
        Component welcomeComponent1 = Component.text(" 才可以正常使用服务器的服务：", NamedTextColor.GOLD);

        Component agreeButton = Component.text("\n[点击同意]", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/agree"));
        Component disagreeButton = Component.text("  [点击不同意]\n", NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/disagree"));
        Component noticeComponent = Component.text("若按钮无法点击，请输入 /agree 或 /disagree 命令", NamedTextColor.GRAY);

        Component finalMessage = welcomeComponent.append(agreementLink)
                .append(welcomeComponent1)
                .append(agreeButton)
                .append(disagreeButton)
                .append(noticeComponent);

        player.sendMessage(finalMessage);
    }
}