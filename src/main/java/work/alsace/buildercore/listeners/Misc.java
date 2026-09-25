package work.alsace.buildercore.listeners;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import static work.alsace.buildercore.BuilderCore.blockedCommands;

public class Misc implements Listener {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    @EventHandler
    public void onBlockFadeEvent(BlockFadeEvent event) {
        if (event.getBlock().getType().name().contains("CORAL")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage();
        if (blockedCommands.contains(command.toLowerCase())) {
            if (event.getPlayer().hasPermission("buildercore.blocked.bypass")) { return; }
            event.setCancelled(true);
            event.getPlayer().sendMessage(MM.deserialize("<white>Unknown command. Type \"/help\" for help."));
        }
    }
}
