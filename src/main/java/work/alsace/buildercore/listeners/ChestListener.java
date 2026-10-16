package work.alsace.buildercore.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Bukkit;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.BlockStateMeta;
import work.alsace.buildercore.BuilderCore;

public class ChestListener implements Listener {

    @EventHandler
    public void onPlayerRightClickAir(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!event.getAction().equals(Action.RIGHT_CLICK_AIR))
            return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR)
            return;
        if (!player.hasPermission("buildercore.service.quickopen"))
            return;
        if (item.getType().getKey().getKey().contains("shulker_box")) {
            BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
            ShulkerBox box = (ShulkerBox) meta.getBlockState();
            player.sendMessage(Component.text("正在打开潜影盒...").color(NamedTextColor.GRAY));
            player.playSound(player, Sound.BLOCK_SHULKER_BOX_OPEN, SoundCategory.BLOCKS, 1.0f, 1.0f);
            PreviewHolder holder = new PreviewHolder();
            holder.inventory = Bukkit.createInventory(holder, InventoryType.SHULKER_BOX, Component.text("潜影盒预览"));
            holder.inventory.setContents(box.getInventory().getContents());
            player.openInventory(holder.inventory);
            player.sendMessage(Component.text("潜影盒仅供预览，无法放入或取出物品").color(NamedTextColor.GRAY));
            BuilderCore.instance.openChests.add(player.getUniqueId());
        } else if (item.getType().equals(Material.ENDER_CHEST)) {
            player.sendMessage(Component.text("正在打开末影箱...").color(NamedTextColor.GRAY));
            player.playSound(player, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.BLOCKS, 1.0f, 1.0f);
            player.openInventory(player.getEnderChest()).setTitle("末影箱");
            BuilderCore.instance.openChests.add(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof PreviewHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof PreviewHolder) {
            event.setCancelled(true);
        }
    }

    private static final class PreviewHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        if (!BuilderCore.instance.openChests.contains(player.getUniqueId()))
            return;
        switch (event.getInventory().getType()) {
            case ENDER_CHEST -> {
                player.playSound(player, Sound.BLOCK_ENDER_CHEST_CLOSE, SoundCategory.BLOCKS, 1.0f, 1.0f);
                player.sendMessage(Component.text("已关闭末影箱").color(NamedTextColor.GRAY));
            }
            case SHULKER_BOX -> {
                player.playSound(player, Sound.BLOCK_SHULKER_BOX_CLOSE, SoundCategory.BLOCKS, 1.0f, 1.0f);
                player.sendMessage(Component.text("已关闭潜影盒").color(NamedTextColor.GRAY));
            }
        }
        BuilderCore.instance.openChests.remove(player.getUniqueId());
    }
}
