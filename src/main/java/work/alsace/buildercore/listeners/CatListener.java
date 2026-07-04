package work.alsace.buildercore.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.HashMap;
import java.util.Map;

public class CatListener implements Listener {
    
    final Map<Cat.Type, Cat.Type> cats = new HashMap<>(11);
    
    public CatListener() {
        cats.put(Cat.Type.BLACK, Cat.Type.BRITISH_SHORTHAIR);
        cats.put(Cat.Type.BRITISH_SHORTHAIR, Cat.Type.CALICO);
        cats.put(Cat.Type.CALICO, Cat.Type.JELLIE);
        cats.put(Cat.Type.JELLIE, Cat.Type.PERSIAN);
        cats.put(Cat.Type.PERSIAN, Cat.Type.RAGDOLL);
        cats.put(Cat.Type.RAGDOLL, Cat.Type.RED);
        cats.put(Cat.Type.RED, Cat.Type.SIAMESE);
        cats.put(Cat.Type.SIAMESE, Cat.Type.TABBY);
        cats.put(Cat.Type.TABBY, Cat.Type.ALL_BLACK);
        cats.put(Cat.Type.ALL_BLACK, Cat.Type.WHITE);
        cats.put(Cat.Type.WHITE, Cat.Type.BLACK);
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() == Material.DEBUG_STICK && player.hasPermission("alsace.protect.place") && player.isSneaking()) {
            if (event.getRightClicked() instanceof Cat cat) {
                event.setCancelled(true);
                Cat.Type newType = getNextCatType(cat.getCatType());
                cat.setCatType(newType);
            }
        } else if (player.getInventory().getItemInMainHand().getType() == Material.DEBUG_STICK && player.hasPermission("alsace.protect.place")) {
            event.setCancelled(true);
            if (event.getRightClicked() instanceof Cat cat) {
                cat.setSitting(!cat.isSitting());
            }
        }
    }

    private Cat.Type getNextCatType(Cat.Type currentType) {
        return cats.get(currentType);
    }

}
