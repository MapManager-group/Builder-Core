package work.alsace.alsacecore.listeners;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import work.alsace.alsacecore.AlsaceCore;
import work.alsace.alsacecore.commands.builderTools.AdvanceFlyCommand;
import work.alsace.alsacecore.commands.teleport.BackCommand;
import work.alsace.alsacecore.service.HomeDataLoader;
import work.alsace.alsacecore.utils.DataBaseManager;
import work.alsace.alsacecore.utils.NoClipUtil;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PlayerListener implements Listener {
    private final AlsaceCore plugin;
    private final DataBaseManager databaseManager;

    public PlayerListener(AlsaceCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        BlockListener.slabs.add(event.getPlayer());
        plugin.homeProfiles.put(event.getPlayer().getUniqueId(), new HomeDataLoader(event.getPlayer().getUniqueId()));
        Player player = event.getPlayer();
        if (plugin.agreement) {
            boolean hasAgreed = databaseManager.hasPlayerAgreed(player.getUniqueId());
            if (!hasAgreed) {
                if (!event.getPlayer().hasPlayedBefore()) {
                    Bukkit.broadcastMessage("§a欢迎新玩家 §f" + event.getPlayer().getName() + " §a加入§f阿尔萨斯§a服务器");
                }
                if (!plugin.hasFloodgate()) {
                    sendAgreementMessage(player);
                } else {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline()) {
                                sendBedrockAgreementMessage(player);
                            }
                        }
                    }.runTaskLater(plugin, 60);
                }

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
        event.setQuitMessage(null);
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
                    player.sendMessage(ChatColor.RED + "你必须先同意用户协议才能使用命令，输入 /agree 进行同意。");

                    if (plugin.hasFloodgate()) {
                        sendBedrockAgreementMessage(player);
                    }
                }
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
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.agreement) {
            if (!databaseManager.hasPlayerAgreed(player.getUniqueId())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "你必须先阅读并同意用户协议才能聊天，输入 /agree 进行同意。");

                if (plugin.hasFloodgate()) {
                    sendBedrockAgreementMessage(player);
                }
            }
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
        TextComponent welcomeComponent = new TextComponent(ChatColor.GOLD + "您必须阅读并同意");

        TextComponent agreementLink = new TextComponent(ChatColor.GREEN + "《工业园用户协议》");
        agreementLink.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://alsaceteam.feishu.cn/wiki/F8AHwoD18iq45fk3ieSc6pcGnYy"));

        TextComponent welcomeComponent1 = new TextComponent(ChatColor.GOLD + "才可以正常使用工业园的服务");

        TextComponent agreeButton = new TextComponent(ChatColor.GREEN + "[同意]");
        agreeButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/agree"));

        TextComponent disagreeButton = new TextComponent(ChatColor.RED + "[不同意]");
        disagreeButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/disagree"));

        TextComponent welcomeComponent2 = new TextComponent(ChatColor.GRAY + "若按钮无法点击，请输入 /agree 命令");


        welcomeComponent.addExtra(agreementLink);
        welcomeComponent.addExtra(welcomeComponent1);
        welcomeComponent.addExtra("\n");
        welcomeComponent.addExtra(agreeButton);
        welcomeComponent.addExtra(" ");
        welcomeComponent.addExtra(disagreeButton);
        welcomeComponent.addExtra(welcomeComponent2);

        player.spigot().sendMessage(welcomeComponent);
    }

    private void sendBedrockAgreementMessage(Player player) {
        FloodgateApi floodgateApi = FloodgateApi.getInstance();
        if (!floodgateApi.isFloodgatePlayer(player.getUniqueId())) { return; }

        FloodgatePlayer floodgatePlayer = floodgateApi.getPlayer(player.getUniqueId());
        floodgatePlayer.sendForm(getAgreementForm(player));
    }

    private CustomForm.Builder getAgreementForm(Player player) {
        return CustomForm.builder()
                .title("工业园用户协议")
                .label("您必须阅读并同意《工业园用户协议》才可以正常使用工业园的服务。")
                .label("§e链接地址: https://alsaceteam.feishu.cn/wiki/F8AHwoD18iq45fk3ieSc6pcGnYy")
                .label("§r请点击下方提交按钮§a同意§r 或 点击右上角关闭按钮§c拒绝§r")
                .validResultHandler(response -> player.performCommand("agree"))
                .closedResultHandler(response -> {
                    if (!plugin.getDatabaseManager().hasPlayerAgreed(player.getUniqueId())) {
                        player.performCommand("disagree");
                    }
                });
    }

}
