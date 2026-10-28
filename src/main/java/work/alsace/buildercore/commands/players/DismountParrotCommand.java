package work.alsace.buildercore.commands.players;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;

public class DismountParrotCommand implements CommandExecutor {

    private static final String PERMISSION = "parrotplugin.dismount";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (work.alsace.buildercore.commands.CommandHelp.requested(sender, command, label, args)) return true;
        // 1. 检查发送者是否为玩家
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("只有玩家才能执行此命令！", NamedTextColor.RED));
            return true;
        }

        // 2. 检查权限
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(Component.text("你没有权限使用此命令！", NamedTextColor.RED));
            return true;
        }

        // 3. 安全获取左右肩膀的 EntitySnapshot
        EntitySnapshot leftShoulder = player.getShoulderEntityLeft() == null ? null : player.getShoulderEntityLeft().createSnapshot();
        EntitySnapshot rightShoulder = player.getShoulderEntityRight() == null ? null : player.getShoulderEntityRight().createSnapshot();

        boolean hasLeft = leftShoulder != null && leftShoulder.getEntityType() == EntityType.PARROT;
        boolean hasRight = rightShoulder != null && rightShoulder.getEntityType() == EntityType.PARROT;

        if (!hasLeft && !hasRight) {
            player.sendMessage(Component.text("你的肩膀上没有鹦鹉！", NamedTextColor.YELLOW));
            return true;
        }

        Location spawnLoc = player.getLocation();

        // 4. 卸下并生成左肩鹦鹉
        if (hasLeft) {
            spawnParrotFromSnapshot(player, leftShoulder, spawnLoc);
            player.setShoulderEntityLeft(null);
        }

        // 5. 卸下并生成右肩鹦鹉
        if (hasRight) {
            spawnParrotFromSnapshot(player, rightShoulder, spawnLoc);
            player.setShoulderEntityRight(null);
        }

        player.sendMessage(Component.text("成功将肩膀上的鹦鹉放了下来！", NamedTextColor.GREEN));
        return true;
    }

    /**
     * 根据 EntitySnapshot 在指定位置还原并在世界中生成鹦鹉实体
     */
    private void spawnParrotFromSnapshot(Player owner, EntitySnapshot snapshot, Location location) {
        World world = location.getWorld();
        if (world == null) return;

        // 利用 EntitySnapshot 生成实体（完整保留颜色、自定义名称、健康值等 NBT 数据）
        Entity entity = snapshot.createEntity(location);

        if (entity instanceof Parrot parrot) {
            // 确保归属与驯服状态，并取消坐下状态
            parrot.setTamed(true);
            parrot.setOwner(owner);
            parrot.setSitting(false);
        }
    }
}