package work.alsace.buildercore.utils;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class NoClipUtil {
    public static Set<Player> noclip = new HashSet<>();

    public void checkBlock() {
        Iterator<Player> players = noclip.iterator();
        while (players.hasNext()) {
            Player id = players.next();
            if (id == null || !id.isOnline()) {
                players.remove();
                continue;
            }

            boolean noClip;
            if (id.getGameMode() == GameMode.CREATIVE) {
                if (id.getLocation().add(0.0, -0.1, 0.0).getBlock().getType().isSolid() && id.isSneaking()) {
                    noClip = true;
                } else {
                    noClip = this.shouldNoClip(id);
                }

                if (noClip) {
                    id.setGameMode(GameMode.SPECTATOR);
                }
            } else if (id.getGameMode() == GameMode.SPECTATOR) {
                if (id.getLocation().add(0.0, -0.1, 0.0).getBlock().getType().isSolid()) {
                    noClip = true;
                } else {
                    noClip = this.shouldNoClip(id);
                }

                if (!noClip) {
                    id.setGameMode(GameMode.CREATIVE);
                }
            }
        }
    }

    private boolean shouldNoClip(Player player) {
        return player.getLocation().add(0.4, 0.0, 0.0).getBlock().getType().isSolid() || player.getLocation().add(-0.4, 0.0, 0.0).getBlock().getType().isSolid() || player.getLocation().add(0.0, 0.0, 0.4).getBlock().getType().isSolid() || player.getLocation().add(0.0, 0.0, -0.4).getBlock().getType().isSolid() || player.getLocation().add(0.4, 1.0, 0.0).getBlock().getType().isSolid() || player.getLocation().add(-0.4, 1.0, 0.0).getBlock().getType().isSolid() || player.getLocation().add(0.0, 1.0, 0.4).getBlock().getType().isSolid() || player.getLocation().add(0.0, 1.0, -0.4).getBlock().getType().isSolid() || player.getLocation().add(0.0, 1.9, 0.0).getBlock().getType().isSolid();
    }
}
