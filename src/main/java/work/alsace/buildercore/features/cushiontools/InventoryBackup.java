package work.alsace.buildercore.features.cushiontools;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.UUID;

/** 先持久化原背包，再允许工具替换；恢复后的玩家数据先保存，再删除备份。 */
final class InventoryBackup {
    private final Path directory;

    InventoryBackup(Path directory) { this.directory = directory; }
    boolean exists(UUID id) { return Files.exists(file(id)); }
    private Path file(UUID id) { return directory.resolve(id + ".dat"); }

    void save(Player player) throws IOException {
        Files.createDirectories(directory);
        Path destination = file(player.getUniqueId());
        if (Files.exists(destination)) throw new IOException("原背包备份尚未恢复");
        Path temporary = Files.createTempFile(directory, "inventory-", ".tmp");
        try {
            try (var output = new DataOutputStream(Files.newOutputStream(temporary))) {
                output.writeInt(1);
                output.writeInt(player.getInventory().getHeldItemSlot());
                ItemStack[] contents = player.getInventory().getContents();
                output.writeInt(contents.length);
                for (ItemStack item : contents) {
                    byte[] bytes = item == null ? new byte[0] : item.serializeAsBytes();
                    output.writeInt(bytes.length);
                    output.write(bytes);
                }
            }
            try (var channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) { channel.force(true); }
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    void restore(Player player) throws IOException {
        Path source = file(player.getUniqueId());
        ItemStack[] contents;
        int slot;
        try (var input = new DataInputStream(Files.newInputStream(source))) {
            if (input.readInt() != 1) throw new IOException("未知背包备份格式");
            slot = input.readInt();
            int size = input.readInt();
            if (slot < 0 || slot > 8 || size != player.getInventory().getContents().length)
                throw new IOException("背包备份长度不匹配");
            contents = new ItemStack[size];
            for (int i = 0; i < size; i++) {
                int length = input.readInt();
                if (length < 0 || length > 16 * 1024 * 1024) throw new IOException("物品备份长度异常");
                byte[] bytes = input.readNBytes(length);
                if (bytes.length != length) throw new EOFException("背包备份不完整");
                if (length != 0) contents[i] = ItemStack.deserializeBytes(bytes);
            }
            if (input.read() != -1) throw new IOException("背包备份有多余数据");
        } catch (RuntimeException exception) {
            throw new IOException("无法读取背包备份", exception);
        }
        player.closeInventory();
        player.setItemOnCursor(null);
        player.getInventory().setContents(contents);
        player.getInventory().setHeldItemSlot(slot);
        player.saveData();
        Files.delete(source);
    }
}
