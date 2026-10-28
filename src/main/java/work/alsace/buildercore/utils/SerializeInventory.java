package work.alsace.buildercore.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.util.Base64;

public class SerializeInventory {
    public String serializeInventory(ItemStack[] inventory) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (BukkitObjectOutputStream data = new BukkitObjectOutputStream(output)) {
            data.writeInt(inventory.length);
            for (ItemStack item : inventory) data.writeObject(item);
        }
        return Base64.getEncoder().encodeToString(output.toByteArray());
    }

    public ItemStack[] deserializeInventory(String encoded) throws IOException {
        try (var data = new BukkitObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(encoded)))) {
            int size = data.readInt();
            if (size < 0 || size > encoded.length()) throw new IOException("无效的背包长度");
            ItemStack[] inventory = new ItemStack[size];
            for (int i = 0; i < size; i++) inventory[i] = (ItemStack) data.readObject();
            return inventory;
        } catch (ClassNotFoundException | IllegalArgumentException | ClassCastException exception) {
            throw new IOException("无法读取背包数据", exception);
        }
    }
}
