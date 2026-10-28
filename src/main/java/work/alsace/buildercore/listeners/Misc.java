package work.alsace.buildercore.listeners;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import static work.alsace.buildercore.BuilderCore.blockedCommands;

public class Misc implements Listener {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    @EventHandler
    public void onBlockFadeEvent(org.bukkit.event.block.BlockFadeEvent event) {
        BlockListener.preventCoralDecay(event);
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().split(" ", 2)[0].toLowerCase(java.util.Locale.ROOT);
        int namespace = command.indexOf(':');
        if (namespace >= 0) command = "/" + command.substring(namespace + 1);
        if (blockedCommands.contains(command)) {
            if (event.getPlayer().hasPermission("buildercore.blocked.bypass")) { return; }
            event.setCancelled(true);
            event.getPlayer().sendMessage(MM.deserialize("<white>Unknown command. Type \"/help\" for help."));
        }
    }
}
