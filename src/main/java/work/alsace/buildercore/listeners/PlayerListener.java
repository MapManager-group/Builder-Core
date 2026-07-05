package work.alsace.buildercore.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PlayerListener implements Listener {
    private final BuilderCore plugin;
    private final DataBaseManager databaseManager;

    public PlayerListener(BuilderCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.joinMessage(Component.empty());
        BlockListener.slabs.add(event.getPlayer());
        plugin.homeProfiles.put(event.getPlayer().getUniqueId(), new HomeDataLoader(event.getPlayer().getUniqueId()));
        Player player = event.getPlayer();
        if (plugin.agreement) {
            boolean hasAgreed = databaseManager.hasPlayerAgreed(player.getUniqueId());
            if (!hasAgreed) {
                if (!event.getPlayer().hasPlayedBefore()) {
                    Component welcomeMessage = Component.text("欢迎新玩家 ", NamedTextColor.GREEN).append(Component.text(event.getPlayer().getName(), NamedTextColor.WHITE)).append(Component.text("加入服务器", NamedTextColor.GREEN));
                }
                sendAgreementMessage(player);

                plugin.hasAgree.put(player.getName(), true); //disagree
            } else {
                plugin.hasAgree.put(player.getName(), false); //agree
            }
        } else {
            plugin.hasAgree.put(player.getName(), false);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.quitMessage(Component.empty());
        Player player = event.getPlayer();
        this.plugin.hasIgnored.remove(player.getName());
        plugin.homeProfiles.remove(player.getUniqueId());
        if (NoClipUtil.noclip.contains(player) || AdvanceFlyCommand.enabledPlayers.contains(player) || BlockListener.slabs.contains(player)) {
            NoClipUtil.noclip.remove(player);
            AdvanceFlyCommand.enabledPlayers.remove(player);
            BlockListener.slabs.remove(player);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (plugin.hasAgree.getOrDefault(player.getName(), false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (plugin.agreement) {
            if (!databaseManager.hasPlayerAgreed(player.getUniqueId())) {
                String message = event.getMessage().toLowerCase();
                if (!message.startsWith("/agree") && !message.startsWith("/disagree")) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("你必须先同意用户协议才能使用命令，输入 /agree 进行同意。", NamedTextColor.RED));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.agreement) {
            if (!databaseManager.hasPlayerAgreed(player.getUniqueId())) {
                event.setCancelled(true);
                player.sendMessage(Component.text("你必须先阅读并同意用户协议才能聊天，输入 /agree 进行同意。", NamedTextColor.RED));
            }
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause().equals(TeleportCause.SPECTATE) && !event.getPlayer().hasPermission("alsace.event.spectate")) {
            event.setCancelled(true);
        }

        // 检查/back命令是否已注册
        if (plugin.getCommand("back") != null) {
            BackCommand backCommand = (BackCommand) Objects.requireNonNull(plugin.getCommand("back")).getExecutor();
            backCommand.addToHistory(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        plugin.homeProfiles.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void playerChangeSign(SignChangeEvent event) {
        boolean cnt = false;
    }

    @EventHandler
    public void playerAnvil(PrepareAnvilEvent event) {
        ItemStack item = event.getResult();
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String name = meta.getDisplayName();
                name = ChatColor.translateAlternateColorCodes('&', name);
                meta.setDisplayName(this.translateHexColorCodes(name));
                item.setItemMeta(meta);
                event.setResult(item);
            }
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

    private String translateHexColorCodes(String message) {
        Pattern hexPattern = Pattern.compile("#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        StringBuilder buffer = new StringBuilder(message.length() + 32);

        while (matcher.find()) {
            String group = matcher.group(1);
            char var10002 = group.charAt(0);
            matcher.appendReplacement(buffer, "§x§" + var10002 + "§" + group.charAt(1) + "§" + group.charAt(2) + "§" + group.charAt(3) + "§" + group.charAt(4) + "§" + group.charAt(5));
        }

        return matcher.appendTail(buffer).toString();
    }

    private void sendAgreementMessage(Player player) {
        Component welcomeComponent = Component.text("您必须阅读并同意", NamedTextColor.GOLD);
        Component agreementLink = Component.text("《" + plugin.getUrlTitle() + "》", NamedTextColor.GREEN).clickEvent(ClickEvent.openUrl(plugin.getUrl()));
        Component welcomeComponent1 = Component.text("才可以正常使用工业园的服务", NamedTextColor.GOLD);

        Component agreeButton = Component.text("[同意]", NamedTextColor.GREEN).clickEvent(ClickEvent.runCommand("/agree"));
        Component disagreeButton = Component.text("[不同意]", NamedTextColor.RED).clickEvent(ClickEvent.runCommand("/disagree"));
        Component welcomeComponent2 = Component.text("若按钮无法点击，请输入 /agree 命令", NamedTextColor.GRAY);
        Component finalMessage = welcomeComponent.append(agreementLink).append(welcomeComponent1).append(Component.newline()).append(agreeButton).append(Component.text(" ")).append(disagreeButton).append(welcomeComponent2);
        player.sendMessage(finalMessage);
    }
}
