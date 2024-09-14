package work.alsace.alsacecore.service;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import work.alsace.alsacecore.AlsaceCore;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class VelocityMessenger implements PluginMessageListener {

    private final AlsaceCore plugin = AlsaceCore.instance;
    private final String channel = "alsace:meta";

    public VelocityMessenger() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, channel);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, channel, this);

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                sendPluginMessage(player,
                        "%multiverse_world_alias%",
                        "%vault_prefix%",
                        "%vault_suffix%",
                        player.hasMetadata("afk")
                );
            }
        }, 0L, 20L);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] data) {
        if (!channel.equals("alsace:meta"))
            return;
    }

    private void sendPluginMessage(Player player, Object... data) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(stream)) {
            for (Object o : data) {
                switch (o) {
                    case String s -> out.writeUTF(PlaceholderAPI.setPlaceholders(player, s));
                    case Integer v -> out.writeInt(v);
                    case Byte b -> out.writeByte(b);
                    case Float v -> out.writeFloat(v);
                    case Double v -> out.writeDouble(v);
                    case Boolean b -> out.writeBoolean(b);
                    case Long v -> out.writeLong(v);
                    case Character c -> out.writeChar(c);
                    case null, default -> {}
                }
            }
            player.sendPluginMessage(plugin, channel, stream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
