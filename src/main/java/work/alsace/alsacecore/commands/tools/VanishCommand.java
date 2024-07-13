package work.alsace.alsacecore.commands.tools;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import work.alsace.alsacecore.AlsaceCore;

public class VanishCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("该命令仅限玩家执行").color(NamedTextColor.RED));
            return true;
        }
        if (!sender.hasPermission("alsace.commands.vanish")) {
            sender.sendMessage(Component.text("你没有使用该命令的权限").color(NamedTextColor.RED));
            return true;
        }
        toggleVanish(player);
        return true;
    }

    private void toggleVanish(Player player) {
        if (player.hasMetadata("vanish")) {
            player.removeMetadata("vanish", AlsaceCore.instance);
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other.getUniqueId().equals(player.getUniqueId()))
                    continue;
                other.showPlayer(AlsaceCore.instance, player);
            }
            player.sendMessage(Component.text("你已取消隐身状态").color(NamedTextColor.GRAY));
        } else {
            player.setMetadata("vanish", new FixedMetadataValue(AlsaceCore.instance, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false));
            if (!AlsaceCore.instance.hasIgnored.getOrDefault(player.getName(), false))
                player.performCommand("tpignore");
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other.getUniqueId().equals(player.getUniqueId()))
                    continue;
                other.hidePlayer(AlsaceCore.instance, player);
            }
            player.sendMessage(Component.text("你已进入隐身状态").color(NamedTextColor.GRAY));
        }
    }
}
